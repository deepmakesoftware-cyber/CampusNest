package com.campusnest.roommate;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusnest.auth.AccountStatus;
import com.campusnest.auth.Role;
import com.campusnest.auth.UserAccount;
import com.campusnest.auth.UserAccountRepository;
import com.campusnest.common.Names;
import com.campusnest.common.error.ApiException;
import com.campusnest.roommate.CompatibilityScorer.Result;
import com.campusnest.roommate.CompatibilityScorer.Side;
import com.campusnest.roommate.RoommateDtos.ConnectionView;
import com.campusnest.roommate.RoommateDtos.MatchView;
import com.campusnest.roommate.RoommateDtos.PersonPreview;
import com.campusnest.roommate.RoommateDtos.ProfileRequest;
import com.campusnest.roommate.RoommateDtos.ProfileView;
import com.campusnest.roommate.RoommateDtos.Relationship;
import com.campusnest.roommate.RoommateDtos.RequestView;
import com.campusnest.roommate.RoommateTypes.RequestStatus;
import com.campusnest.student.StudentProfile;
import com.campusnest.student.StudentProfileRepository;

/**
 * Roommate discovery with privacy built in:
 * - Only students who opted in (discoverable) appear, and only to other discoverable students (reciprocity).
 * - Before a connection is accepted by BOTH sides, a student sees only a first name + last initial, course, year,
 *   college and the preferences that explain the score. No email, phone or full name.
 * - Blocking hides both students from each other and stops requests.
 * - At most MAX_REQUESTS_PER_DAY new requests a day (anti-spam).
 */
@Service
public class RoommateService {

    static final int MAX_REQUESTS_PER_DAY = 10;

    private final RoommateProfileRepository profiles;
    private final RoommateRequestRepository requests;
    private final RoommateBlockRepository blocks;
    private final StudentProfileRepository studentProfiles;
    private final UserAccountRepository users;

    public RoommateService(RoommateProfileRepository profiles, RoommateRequestRepository requests,
            RoommateBlockRepository blocks, StudentProfileRepository studentProfiles, UserAccountRepository users) {
        this.profiles = profiles;
        this.requests = requests;
        this.blocks = blocks;
        this.studentProfiles = studentProfiles;
        this.users = users;
    }

    // ---------------- own profile ----------------

    @Transactional(readOnly = true)
    public Optional<ProfileView> getProfile(Long userId) {
        return profiles.findById(userId).map(ProfileView::from);
    }

    @Transactional
    public ProfileView saveProfile(Long userId, ProfileRequest r) {
        if (r.budgetMin() > r.budgetMax()) {
            throw ApiException.badRequest("INVALID_BUDGET", "The minimum budget cannot be more than the maximum.");
        }
        RoommateProfile p = profiles.findById(userId).orElseGet(() -> new RoommateProfile(users.getReferenceById(userId)));
        p.update(r.discoverable(), r.budgetMin(), r.budgetMax(), r.preferredLocalities().stream().map(String::trim)
                .filter(s -> !s.isEmpty()).distinct().toList(), r.anywhereNearCollege(), r.sleepSchedule(), r.smoking(),
                r.cannotLiveWithSmoker(), r.studyEnvironment(), r.cleanliness(), r.guests(), r.foodPreference(),
                r.sharingSize(), r.sameGenderOnly(), r.about());
        return ProfileView.from(profiles.save(p));
    }

    // ---------------- matches ----------------

    @Transactional(readOnly = true)
    public List<MatchView> matches(Long viewerId) {
        RoommateProfile mine = requireDiscoverable(viewerId);
        Set<Long> hidden = blockedEitherWay(viewerId);
        Map<Long, RoommateRequest> active = activeRequestsByOtherUser(viewerId);
        List<RoommateProfile> others = profiles.findDiscoverable().stream()
                .filter(p -> !p.getUserId().equals(viewerId) && !hidden.contains(p.getUserId()))
                .filter(p -> p.getUser().getRole() == Role.STUDENT)
                .toList();
        Map<Long, StudentProfile> students = studentProfiles.findAllById(
                concat(others.stream().map(RoommateProfile::getUserId).toList(), viewerId)).stream()
                .collect(Collectors.toMap(StudentProfile::getUserId, Function.identity()));
        Side me = Side.of(mine, genderOf(students, viewerId));

        return others.stream()
                .map(p -> {
                    Result result = CompatibilityScorer.score(me, Side.of(p, genderOf(students, p.getUserId())));
                    return new MatchView(preview(p, students.get(p.getUserId())), result,
                            relationship(active.get(p.getUserId()), viewerId), requestId(active.get(p.getUserId())));
                })
                .filter(m -> m.compatibility().compatible())
                .sorted(Comparator.comparing((MatchView m) -> m.compatibility().percent()).reversed())
                .toList();
    }

    // ---------------- requests ----------------

    @Transactional
    public RequestView sendRequest(Long fromId, Long toId, String message) {
        if (fromId.equals(toId)) {
            throw ApiException.badRequest("INVALID_REQUEST", "You cannot send a request to yourself.");
        }
        RoommateProfile mine = requireDiscoverable(fromId);
        RoommateProfile theirs = profiles.findById(toId)
                .filter(RoommateProfile::isDiscoverable)
                .filter(p -> p.getUser().getStatus() == AccountStatus.ACTIVE)
                .filter(p -> !blockedEitherWay(fromId).contains(toId))
                .orElseThrow(() -> ApiException.notFound("STUDENT_NOT_FOUND", "This student is not available for roommate matching."));
        Map<Long, StudentProfile> students = studentProfiles.findAllById(List.of(fromId, toId)).stream()
                .collect(Collectors.toMap(StudentProfile::getUserId, Function.identity()));
        Result result = CompatibilityScorer.score(Side.of(mine, genderOf(students, fromId)), Side.of(theirs, genderOf(students, toId)));
        if (!result.compatible()) {
            // Respect the other student's stated dealbreakers (e.g. same gender only, no smokers).
            throw ApiException.conflict("NOT_COMPATIBLE", "Your preferences and theirs rule this match out.");
        }
        if (!requests.findActiveBetween(fromId, toId).isEmpty()) {
            throw ApiException.conflict("REQUEST_EXISTS", "You already have a pending request or connection with this student.");
        }
        if (requests.countByFromIdAndCreatedAtAfter(fromId, Instant.now().minus(Duration.ofHours(24))) >= MAX_REQUESTS_PER_DAY) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS",
                    "You have sent many requests today. Please try again tomorrow.");
        }
        RoommateRequest saved = requests.save(new RoommateRequest(users.getReferenceById(fromId), theirs.getUser(), message));
        return toRequestView(saved, fromId, students);
    }

    @Transactional
    public RequestView respond(Long requestId, Long userId, String action) {
        RoommateRequest r = requests.findById(requestId)
                .filter(x -> x.involves(userId))
                .orElseThrow(() -> ApiException.notFound("REQUEST_NOT_FOUND", "Request not found."));
        if (r.getStatus() != RequestStatus.PENDING) {
            throw ApiException.conflict("REQUEST_CLOSED", "This request has already been answered.");
        }
        boolean recipient = r.getTo().getId().equals(userId);
        switch (action) {
            case "accept" -> {
                requireRecipient(recipient);
                r.respond(RequestStatus.ACCEPTED);
            }
            case "decline" -> {
                requireRecipient(recipient);
                r.respond(RequestStatus.DECLINED);
            }
            case "withdraw" -> {
                if (recipient) {
                    throw ApiException.conflict("NOT_ALLOWED", "Only the student who sent a request can withdraw it.");
                }
                r.respond(RequestStatus.WITHDRAWN);
            }
            default -> throw ApiException.badRequest("INVALID_ACTION", "Unknown action.");
        }
        Map<Long, StudentProfile> students = studentProfiles.findAllById(List.of(r.getFrom().getId(), r.getTo().getId())).stream()
                .collect(Collectors.toMap(StudentProfile::getUserId, Function.identity()));
        return toRequestView(r, userId, students);
    }

    /** Pending requests the student sent or received. */
    @Transactional(readOnly = true)
    public List<RequestView> pendingRequests(Long userId) {
        List<RoommateRequest> pending = requests.findInvolving(userId).stream()
                .filter(r -> r.getStatus() == RequestStatus.PENDING).toList();
        Map<Long, StudentProfile> students = studentsFor(pending);
        return pending.stream().map(r -> toRequestView(r, userId, students)).toList();
    }

    /** Accepted connections: ONLY here are full names and contact details shown. */
    @Transactional(readOnly = true)
    public List<ConnectionView> connections(Long userId) {
        List<RoommateRequest> accepted = requests.findInvolving(userId).stream()
                .filter(r -> r.getStatus() == RequestStatus.ACCEPTED).toList();
        Map<Long, StudentProfile> students = studentsFor(accepted);
        return accepted.stream().map(r -> {
            UserAccount other = r.getFrom().getId().equals(userId) ? r.getTo() : r.getFrom();
            StudentProfile sp = students.get(other.getId());
            return new ConnectionView(r.getId(), other.getId(), other.getDisplayName(), other.getEmail(), other.getPhone(),
                    sp != null && sp.getCollege() != null ? sp.getCollege().getShortName() : null,
                    sp == null ? null : sp.getCourse(), r.getRespondedAt());
        }).toList();
    }

    // ---------------- blocking ----------------

    @Transactional
    public void block(Long blockerId, Long blockedId) {
        if (blockerId.equals(blockedId)) {
            throw ApiException.badRequest("INVALID_REQUEST", "You cannot block yourself.");
        }
        UserAccount blocked = users.findById(blockedId)
                .filter(u -> u.getRole() == Role.STUDENT)
                .orElseThrow(() -> ApiException.notFound("STUDENT_NOT_FOUND", "Student not found."));
        if (!blocks.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            blocks.save(new RoommateBlock(users.getReferenceById(blockerId), blocked));
        }
        // Close anything open between the two of them.
        for (RoommateRequest r : requests.findActiveBetween(blockerId, blockedId)) {
            r.respond(r.getFrom().getId().equals(blockerId) ? RequestStatus.WITHDRAWN : RequestStatus.DECLINED);
        }
    }

    // ---------------- helpers ----------------

    private RoommateProfile requireDiscoverable(Long userId) {
        return profiles.findById(userId).filter(RoommateProfile::isDiscoverable)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "ROOMMATE_PROFILE_REQUIRED",
                        "Create your roommate profile and make it visible to see matches."));
    }

    private static void requireRecipient(boolean recipient) {
        if (!recipient) {
            throw ApiException.conflict("NOT_ALLOWED", "Only the student who received the request can answer it.");
        }
    }

    private Set<Long> blockedEitherWay(Long userId) {
        Set<Long> ids = new HashSet<>();
        for (RoommateBlock b : blocks.findInvolving(userId)) {
            ids.add(b.getBlocker().getId().equals(userId) ? b.getBlocked().getId() : b.getBlocker().getId());
        }
        return ids;
    }

    private Map<Long, RoommateRequest> activeRequestsByOtherUser(Long userId) {
        return requests.findInvolving(userId).stream()
                .filter(RoommateRequest::isActive)
                .collect(Collectors.toMap(r -> r.getFrom().getId().equals(userId) ? r.getTo().getId() : r.getFrom().getId(),
                        Function.identity(), (x, y) -> x));
    }

    private static Relationship relationship(RoommateRequest r, Long viewerId) {
        if (r == null) {
            return Relationship.NONE;
        }
        if (r.getStatus() == RequestStatus.ACCEPTED) {
            return Relationship.CONNECTED;
        }
        return r.getFrom().getId().equals(viewerId) ? Relationship.REQUEST_SENT : Relationship.REQUEST_RECEIVED;
    }

    private static Long requestId(RoommateRequest r) {
        return r == null ? null : r.getId();
    }

    private RequestView toRequestView(RoommateRequest r, Long viewerId, Map<Long, StudentProfile> students) {
        boolean sent = r.getFrom().getId().equals(viewerId);
        UserAccount other = sent ? r.getTo() : r.getFrom();
        RoommateProfile otherProfile = profiles.findById(other.getId()).orElse(null);
        return new RequestView(r.getId(), sent ? "SENT" : "RECEIVED", r.getStatus(), r.getMessage(), r.getCreatedAt(),
                otherProfile == null ? null : preview(otherProfile, students.get(other.getId())));
    }

    private Map<Long, StudentProfile> studentsFor(List<RoommateRequest> list) {
        Set<Long> ids = new HashSet<>();
        list.forEach(r -> {
            ids.add(r.getFrom().getId());
            ids.add(r.getTo().getId());
        });
        return studentProfiles.findAllById(ids).stream()
                .collect(Collectors.toMap(StudentProfile::getUserId, Function.identity()));
    }

    private static com.campusnest.student.Gender genderOf(Map<Long, StudentProfile> students, Long userId) {
        StudentProfile sp = students.get(userId);
        return sp == null ? null : sp.getGender();
    }

    /** The privacy-limited view of another student. */
    static PersonPreview preview(RoommateProfile p, StudentProfile sp) {
        return new PersonPreview(p.getUserId(), shortName(p.getUser().getDisplayName()),
                sp != null && sp.getCollege() != null ? sp.getCollege().getShortName() : null,
                sp == null ? null : sp.getCourse(), sp == null ? null : sp.getYearOfStudy(), p.getAbout(),
                p.getSleepSchedule(), p.getStudyEnvironment(), p.getCleanliness(), p.getGuests(), p.getSmoking(),
                p.getFoodPreference(), p.getSharingSize(),
                p.isAnywhereNearCollege() ? List.of() : p.getPreferredLocalities(), p.isAnywhereNearCollege());
    }

    static String shortName(String displayName) {
        return Names.shortName(displayName);
    }

    private static List<Long> concat(List<Long> ids, Long extra) {
        List<Long> all = new java.util.ArrayList<>(ids);
        all.add(extra);
        return all;
    }
}

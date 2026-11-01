package com.campusnest.engagement;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusnest.auth.UserAccount;
import com.campusnest.auth.UserAccountRepository;
import com.campusnest.common.error.ApiException;
import com.campusnest.listing.Listing;
import com.campusnest.listing.ListingRepository;
import com.campusnest.listing.PublicListingService;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Private student ↔ owner conversations about a listing.
 *
 * Privacy rules:
 * - Only the two participants can read a thread; anyone else gets 404 (the thread's existence is not revealed).
 * - Phone and email are shown to the other person ONLY if their owner chose "share my contact details".
 *
 * Abuse limit: a student can start at most MAX_NEW_PER_DAY new enquiries in 24 hours.
 */
@Service
public class EnquiryService {

    static final int MAX_NEW_PER_DAY = 15;

    private final EnquiryRepository enquiries;
    private final ListingRepository listings;
    private final UserAccountRepository users;

    public EnquiryService(EnquiryRepository enquiries, ListingRepository listings, UserAccountRepository users) {
        this.enquiries = enquiries;
        this.listings = listings;
        this.users = users;
    }

    /** Starts a thread, or adds to the existing one if this student already asked about this listing. */
    @Transactional
    public EnquiryView start(Long studentId, StartEnquiryRequest request) {
        Listing listing = listings.findById(request.listingId())
                .filter(PublicListingService::isVisible)
                .orElseThrow(() -> ApiException.notFound("LISTING_NOT_FOUND", "Listing not found."));
        UserAccount student = users.getReferenceById(studentId);

        Enquiry enquiry = enquiries.findByListingIdAndStudentId(listing.getId(), studentId).orElse(null);
        if (enquiry == null) {
            Instant dayAgo = Instant.now().minus(Duration.ofHours(24));
            if (enquiries.countByStudentIdAndCreatedAtAfter(studentId, dayAgo) >= MAX_NEW_PER_DAY) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_ENQUIRIES",
                        "You have contacted many owners today. Please try again tomorrow.");
            }
            enquiry = enquiries.save(new Enquiry(listing, student));
        }
        if (request.sharesContact()) {
            enquiry.studentSharesContact();
        }
        enquiry.addMessage(student, request.message());
        return toView(enquiry, studentId);
    }

    @Transactional
    public EnquiryView reply(Long enquiryId, Long userId, ReplyRequest request) {
        Enquiry enquiry = loadForParticipant(enquiryId, userId);
        boolean isStudent = enquiry.getStudent().getId().equals(userId);
        if (request.sharesContact()) {
            if (isStudent) {
                enquiry.studentSharesContact();
            } else {
                enquiry.ownerSharesContact();
            }
        }
        enquiry.addMessage(users.getReferenceById(userId), request.message());
        return toView(enquiry, userId);
    }

    @Transactional(readOnly = true)
    public EnquiryView get(Long enquiryId, Long userId) {
        return toView(loadForParticipant(enquiryId, userId), userId);
    }

    @Transactional(readOnly = true)
    public List<EnquirySummary> listForStudent(Long studentId) {
        return enquiries.findByStudentIdOrderByLastMessageAtDesc(studentId).stream()
                .map(e -> toSummary(e, studentId)).toList();
    }

    @Transactional(readOnly = true)
    public List<EnquirySummary> listForOwner(Long ownerId) {
        return enquiries.findByListingOwnerIdOrderByLastMessageAtDesc(ownerId).stream()
                .map(e -> toSummary(e, ownerId)).toList();
    }

    // ---------------- helpers ----------------

    private Enquiry loadForParticipant(Long enquiryId, Long userId) {
        return enquiries.findById(enquiryId)
                .filter(e -> e.involves(userId))
                .orElseThrow(() -> ApiException.notFound("ENQUIRY_NOT_FOUND", "Conversation not found."));
    }

    private static EnquiryView toView(Enquiry e, Long viewerId) {
        boolean viewerIsStudent = e.getStudent().getId().equals(viewerId);
        UserAccount other = viewerIsStudent ? e.getListing().getOwner() : e.getStudent();
        boolean otherShares = viewerIsStudent ? e.isOwnerSharesContact() : e.isStudentSharesContact();
        boolean iShare = viewerIsStudent ? e.isStudentSharesContact() : e.isOwnerSharesContact();
        Listing l = e.getListing();
        return new EnquiryView(e.getId(), l.getId(), l.getTitle(), l.getLocality(),
                viewerIsStudent ? "STUDENT" : "OWNER",
                other.getDisplayName(),
                otherShares ? other.getEmail() : null,
                otherShares ? other.getPhone() : null,
                iShare,
                e.getMessages().stream().map(m -> new MessageView(m.getId(), m.getSender().getId().equals(viewerId),
                        m.getSender().getDisplayName(), m.getBody(), m.getCreatedAt())).toList(),
                e.getLastMessageAt());
    }

    private static EnquirySummary toSummary(Enquiry e, Long viewerId) {
        boolean viewerIsStudent = e.getStudent().getId().equals(viewerId);
        UserAccount other = viewerIsStudent ? e.getListing().getOwner() : e.getStudent();
        List<EnquiryMessage> messages = e.getMessages();
        EnquiryMessage last = messages.isEmpty() ? null : messages.get(messages.size() - 1);
        String preview = last == null ? "" : (last.getBody().length() > 80 ? last.getBody().substring(0, 80) + "…" : last.getBody());
        boolean waitingForMe = last != null && !last.getSender().getId().equals(viewerId);
        return new EnquirySummary(e.getId(), e.getListing().getId(), e.getListing().getTitle(), other.getDisplayName(),
                preview, waitingForMe, messages.size(), e.getLastMessageAt());
    }

    // ---------------- DTOs ----------------

    // shareContact is a Boolean (not boolean) so it may be left out: missing means "do not share".
    public record StartEnquiryRequest(
            @NotNull Long listingId,
            @NotBlank @Size(max = 2000) String message,
            Boolean shareContact) {

        boolean sharesContact() {
            return Boolean.TRUE.equals(shareContact);
        }
    }

    public record ReplyRequest(@NotBlank @Size(max = 2000) String message, Boolean shareContact) {

        boolean sharesContact() {
            return Boolean.TRUE.equals(shareContact);
        }
    }

    public record MessageView(Long id, boolean fromMe, String senderName, String body, Instant at) {
    }

    /** otherEmail / otherPhone are null unless the other person chose to share them. */
    public record EnquiryView(Long id, Long listingId, String listingTitle, String listingLocality, String myRole,
            String otherName, String otherEmail, String otherPhone, boolean iShareContact,
            List<MessageView> messages, Instant lastMessageAt) {
    }

    public record EnquirySummary(Long id, Long listingId, String listingTitle, String otherName, String lastMessage,
            boolean waitingForMyReply, int messageCount, Instant lastMessageAt) {
    }
}

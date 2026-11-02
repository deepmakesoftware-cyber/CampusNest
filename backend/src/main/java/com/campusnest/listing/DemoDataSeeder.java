package com.campusnest.listing;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.campusnest.auth.Role;
import com.campusnest.auth.UserAccount;
import com.campusnest.auth.UserAccountRepository;
import com.campusnest.college.CollegeRepository;
import com.campusnest.common.storage.ImageStorage;
import com.campusnest.listing.ListingTypes.ChargeFrequency;
import com.campusnest.listing.ListingTypes.ChargeType;
import com.campusnest.listing.ListingTypes.GenderPolicy;
import com.campusnest.listing.ListingTypes.PropertyType;
import com.campusnest.listing.ListingTypes.RoomType;
import com.campusnest.owner.OwnerProfile;
import com.campusnest.owner.OwnerProfileRepository;
import com.campusnest.owner.OwnerType;
import com.campusnest.commute.TravelMode;
import com.campusnest.roommate.RoommateProfile;
import com.campusnest.roommate.RoommateProfileRepository;
import com.campusnest.roommate.RoommateTypes.FoodPreference;
import com.campusnest.roommate.RoommateTypes.SharingSize;
import com.campusnest.roommate.RoommateTypes.SleepSchedule;
import com.campusnest.roommate.RoommateTypes.Smoking;
import com.campusnest.roommate.RoommateTypes.StudyEnvironment;
import com.campusnest.student.Gender;
import com.campusnest.student.StudentPreferences;
import com.campusnest.student.StudentPreferencesRepository;
import com.campusnest.student.StudentProfile;
import com.campusnest.student.StudentProfileRepository;

/**
 * Creates realistic SAMPLE data for demonstrations, only when DEMO_DATA=true (off by default, never in tests).
 *
 * Honesty rules followed here:
 * - Every listing is flagged sampleData = true and shown with a "Sample listing" badge.
 * - All people are invented, with @example.com emails (a domain reserved for examples).
 * - Addresses say "Sample address"; coordinates are approximate neighbourhood centres, not real buildings.
 * - Photos are generated images that say "SAMPLE PHOTO".
 * - Rents are plausible for student accommodation in these areas but are NOT market data.
 * Listings go through the real state machine (draft -> review -> published), so their history is genuine.
 *
 * Runs once: if any sample listing already exists, it does nothing.
 */
@Component
@Order(20) // after AdminAccountInitializer, so the admin can be recorded as the approver
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final boolean enabled;
    private final String demoPassword;
    private final String adminEmail;
    private final UserAccountRepository users;
    private final OwnerProfileRepository ownerProfiles;
    private final StudentProfileRepository studentProfiles;
    private final StudentPreferencesRepository studentPreferences;
    private final RoommateProfileRepository roommateProfiles;
    private final CollegeRepository colleges;
    private final ListingRepository listings;
    private final AmenityRepository amenities;
    private final ListingLifecycleService lifecycle;
    private final ImageStorage imageStorage;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(@Value("${campusnest.demo-data.enabled:false}") boolean enabled,
            @Value("${campusnest.demo-data.password:}") String demoPassword,
            @Value("${campusnest.admin.email:}") String adminEmail,
            UserAccountRepository users, OwnerProfileRepository ownerProfiles,
            StudentProfileRepository studentProfiles, StudentPreferencesRepository studentPreferences,
            RoommateProfileRepository roommateProfiles, CollegeRepository colleges, ListingRepository listings, AmenityRepository amenities,
            ListingLifecycleService lifecycle, ImageStorage imageStorage, PasswordEncoder passwordEncoder) {
        this.enabled = enabled;
        this.demoPassword = demoPassword;
        this.adminEmail = UserAccount.normalizeEmail(adminEmail);
        this.users = users;
        this.ownerProfiles = ownerProfiles;
        this.studentProfiles = studentProfiles;
        this.studentPreferences = studentPreferences;
        this.roommateProfiles = roommateProfiles;
        this.colleges = colleges;
        this.listings = listings;
        this.amenities = amenities;
        this.lifecycle = lifecycle;
        this.imageStorage = imageStorage;
        this.passwordEncoder = passwordEncoder;
    }

    // ---- the sample data (invented) ----

    private record Room(RoomType type, int sharing, int rent, int deposit, int total, int free) {
    }

    private record Charge(ChargeType type, int amount, ChargeFrequency frequency, boolean mandatory) {
    }

    private record Sample(int owner, String title, PropertyType type, GenderPolicy gender, String locality,
            String pincode, double lat, double lng, int confirmedDaysAgo, List<Room> rooms, List<Charge> charges,
            Set<String> yes, Color colour) {
    }

    private static final Set<String> CORE = Set.of("WIFI", "MEALS", "AC", "ATTACHED_BATHROOM", "HOT_WATER", "LAUNDRY",
            "SECURITY", "POWER_BACKUP");

    private static final List<Sample> SAMPLES = List.of(
            new Sample(0, "Green Nest PG", PropertyType.PG, GenderPolicy.ANY, "Matunga East", "400019", 19.0262, 72.8566, 2,
                    List.of(new Room(RoomType.DOUBLE, 2, 9500, 19000, 6, 2), new Room(RoomType.SINGLE, 1, 14000, 28000, 2, 1)),
                    List.of(new Charge(ChargeType.ELECTRICITY, 800, ChargeFrequency.MONTHLY, true)),
                    Set.of("WIFI", "MEALS", "HOT_WATER", "LAUNDRY", "SECURITY", "POWER_BACKUP", "STUDY_TABLE", "DRINKING_WATER"),
                    new Color(0x2F855A)),
            new Sample(1, "Kings Circle Girls Hostel", PropertyType.HOSTEL, GenderPolicy.FEMALE_ONLY, "King's Circle", "400019", 19.0305, 72.8575, 5,
                    List.of(new Room(RoomType.TRIPLE, 3, 7500, 15000, 9, 3), new Room(RoomType.DOUBLE, 2, 9000, 18000, 4, 1)),
                    List.of(),
                    Set.of("WIFI", "MEALS", "HOT_WATER", "SECURITY", "POWER_BACKUP", "HOUSEKEEPING", "DRINKING_WATER"),
                    new Color(0xB83280)),
            new Sample(2, "Dadar TT Shared Flat", PropertyType.FLAT, GenderPolicy.ANY, "Dadar East", "400014", 19.0186, 72.8445, 25,
                    List.of(new Room(RoomType.DOUBLE, 2, 12000, 36000, 4, 2)),
                    List.of(new Charge(ChargeType.MAINTENANCE, 1000, ChargeFrequency.MONTHLY, true),
                            new Charge(ChargeType.ELECTRICITY, 1200, ChargeFrequency.MONTHLY, true)),
                    Set.of("WIFI", "AC", "ATTACHED_BATHROOM", "HOT_WATER", "LAUNDRY", "KITCHEN_ACCESS", "REFRIGERATOR", "LIFT"),
                    new Color(0x2B6CB0)),
            new Sample(3, "Wadala Budget Rooms", PropertyType.ROOM, GenderPolicy.MALE_ONLY, "Wadala", "400031", 19.0176, 72.8640, 40,
                    List.of(new Room(RoomType.TRIPLE, 3, 6000, 12000, 6, 2)),
                    List.of(new Charge(ChargeType.ELECTRICITY, 600, ChargeFrequency.MONTHLY, true)),
                    Set.of("HOT_WATER", "SECURITY"),
                    new Color(0x975A16)),
            new Sample(0, "Sion Student Residency", PropertyType.HOSTEL, GenderPolicy.ANY, "Sion West", "400022", 19.0430, 72.8620, 1,
                    List.of(new Room(RoomType.SINGLE, 1, 15000, 30000, 4, 1), new Room(RoomType.DOUBLE, 2, 10500, 21000, 10, 4)),
                    List.of(new Charge(ChargeType.FOOD, 3500, ChargeFrequency.MONTHLY, false)),
                    Set.of("WIFI", "AC", "ATTACHED_BATHROOM", "HOT_WATER", "LAUNDRY", "SECURITY", "POWER_BACKUP", "STUDY_TABLE", "LIFT"),
                    new Color(0x553C9A)),
            new Sample(1, "Parel Comfort PG", PropertyType.PG, GenderPolicy.ANY, "Parel", "400012", 19.0020, 72.8420, 10,
                    List.of(new Room(RoomType.DOUBLE, 2, 11000, 22000, 6, 2)),
                    List.of(new Charge(ChargeType.ELECTRICITY, 900, ChargeFrequency.MONTHLY, true)),
                    Set.of("WIFI", "MEALS", "AC", "HOT_WATER", "LAUNDRY", "SECURITY"),
                    new Color(0x2C7A7B)),
            new Sample(2, "Chembur Homestay for Women", PropertyType.PG, GenderPolicy.FEMALE_ONLY, "Chembur", "400071", 19.0620, 72.8990, 12,
                    List.of(new Room(RoomType.DOUBLE, 2, 8500, 17000, 4, 2)),
                    List.of(),
                    Set.of("WIFI", "MEALS", "HOT_WATER", "SECURITY", "KITCHEN_ACCESS"),
                    new Color(0xC05621)),
            new Sample(3, "Kurla West Hostel", PropertyType.HOSTEL, GenderPolicy.MALE_ONLY, "Kurla West", "400070", 19.0700, 72.8790, 8,
                    List.of(new Room(RoomType.DORMITORY, 6, 5000, 10000, 12, 5)),
                    List.of(new Charge(ChargeType.MAINTENANCE, 300, ChargeFrequency.MONTHLY, true)),
                    Set.of("WIFI", "HOT_WATER", "SECURITY", "POWER_BACKUP"),
                    new Color(0x4A5568)),
            new Sample(0, "Ghatkopar East PG", PropertyType.PG, GenderPolicy.ANY, "Ghatkopar East", "400077", 19.0860, 72.9080, 3,
                    List.of(new Room(RoomType.DOUBLE, 2, 7000, 14000, 6, 3)),
                    List.of(new Charge(ChargeType.ELECTRICITY, 700, ChargeFrequency.MONTHLY, true)),
                    Set.of("WIFI", "MEALS", "HOT_WATER", "LAUNDRY", "SECURITY"),
                    new Color(0x2F855A)),
            new Sample(1, "Mahim Causeway Rooms", PropertyType.ROOM, GenderPolicy.ANY, "Mahim West", "400016", 19.0410, 72.8400, 60,
                    List.of(new Room(RoomType.SINGLE, 1, 13000, 39000, 2, 0)),
                    List.of(),
                    Set.of("ATTACHED_BATHROOM", "HOT_WATER"),
                    new Color(0x718096)),
            new Sample(2, "Andheri East Co-living", PropertyType.FLAT, GenderPolicy.ANY, "Andheri East", "400069", 19.1150, 72.8700, 4,
                    List.of(new Room(RoomType.SINGLE, 1, 12500, 25000, 3, 1), new Room(RoomType.DOUBLE, 2, 9000, 18000, 4, 2)),
                    List.of(new Charge(ChargeType.WIFI, 500, ChargeFrequency.MONTHLY, true)),
                    Set.of("WIFI", "AC", "ATTACHED_BATHROOM", "HOT_WATER", "LAUNDRY", "SECURITY", "POWER_BACKUP", "HOUSEKEEPING", "LIFT"),
                    new Color(0x2B6CB0)),
            new Sample(3, "Thane Budget PG", PropertyType.PG, GenderPolicy.ANY, "Thane West", "400601", 19.1860, 72.9750, 6,
                    List.of(new Room(RoomType.TRIPLE, 3, 5500, 11000, 9, 4)),
                    List.of(new Charge(ChargeType.ELECTRICITY, 500, ChargeFrequency.MONTHLY, true)),
                    Set.of("WIFI", "MEALS", "HOT_WATER", "SECURITY"),
                    new Color(0x975A16)));

    private record DemoOwner(String name, String email, OwnerType type, String business, boolean verified) {
    }

    private static final List<DemoOwner> OWNERS = List.of(
            new DemoOwner("Ramesh Iyer (sample)", "owner.ramesh@example.com", OwnerType.PG_OPERATOR, "Sample Homes", true),
            new DemoOwner("Sunita Desai (sample)", "owner.sunita@example.com", OwnerType.HOSTEL_OPERATOR, "Sample Hostels", true),
            new DemoOwner("Imran Shaikh (sample)", "owner.imran@example.com", OwnerType.INDIVIDUAL, null, false),
            new DemoOwner("Prakash Patil (sample)", "owner.prakash@example.com", OwnerType.INDIVIDUAL, null, false));

    // ---- seeding ----

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        if (demoPassword == null || demoPassword.length() < 12) {
            log.warn("DEMO_DATA is on but DEMO_PASSWORD is missing or shorter than 12 characters: demo data NOT created.");
            return;
        }
        ensureDemoStudent(); // idempotent: also fills in data added in later phases (e.g. preferences)
        ensureDemoRoommates();
        if (listings.existsBySampleDataTrue()) {
            log.info("Sample listings already exist: demo listings not re-created.");
            return;
        }

        UserAccount admin = adminEmail == null ? null : users.findByEmail(adminEmail).orElse(null);
        Instant now = Instant.now();

        List<UserAccount> owners = OWNERS.stream().map(o -> {
            UserAccount account = users.findByEmail(o.email()).orElseGet(() ->
                    users.save(new UserAccount(o.email(), passwordEncoder.encode(demoPassword), Role.OWNER, o.name())));
            OwnerProfile profile = ownerProfiles.findById(account.getId())
                    .orElseGet(() -> ownerProfiles.save(new OwnerProfile(account)));
            profile.update(o.type(), o.business());
            if (o.verified() && admin != null) {
                profile.markVerified(admin, now.minus(Duration.ofDays(20)));
            }
            return account;
        }).toList();

        Map<String, Amenity> amenityByCode = amenities.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(Amenity::getCode, a -> a));

        for (Sample s : SAMPLES) {
            UserAccount owner = owners.get(s.owner());
            Listing l = listings.save(new Listing(owner, s.title()));
            lifecycle.recordCreated(l, owner);
            l.markAsSampleData();
            l.updateDetails(s.title(),
                    "Sample listing created for CampusNest demonstrations. " + s.type().name().charAt(0)
                            + s.type().name().substring(1).toLowerCase() + " accommodation in " + s.locality()
                            + ". Details, rent and photos are illustrative, not a real property.",
                    s.type(), s.gender(), "Sample address, " + s.locality(), s.locality(), s.pincode(),
                    BigDecimal.valueOf(s.lat()), BigDecimal.valueOf(s.lng()));
            for (Room r : s.rooms()) {
                RoomOption room = new RoomOption(l);
                room.update(r.type(), r.sharing(), r.rent(), r.deposit(), r.total(), r.free(), now);
                l.rooms().add(room);
            }
            for (Charge c : s.charges()) {
                l.charges().add(new ListingCharge(l, c.type(), null, c.amount(), c.frequency(), c.mandatory()));
            }
            // Core amenities are always answered (yes/no); non-core ones only when "yes".
            for (Amenity a : amenityByCode.values()) {
                boolean yes = s.yes().contains(a.getCode());
                if (yes || CORE.contains(a.getCode())) {
                    l.amenityAnswers().add(new ListingAmenity(l, a, yes));
                }
            }
            byte[] photo = samplePhoto(s.title(), s.locality(), s.colour());
            String key = imageStorage.save(photo, "png");
            l.imageList().add(new ListingImage(l, key, "image/png", photo.length, 800, 600));

            lifecycle.transition(l, ListingStatus.PENDING_VERIFICATION, owner, null);
            lifecycle.approve(l, admin);
            l.confirmAvailability(now.minus(Duration.ofDays(s.confirmedDaysAgo())));
        }
        log.info("Created {} sample listings, {} sample owners and 1 sample student (all marked as sample data).",
                SAMPLES.size(), OWNERS.size());
    }

    /** Sample student "Asha": VJTI, MCA year 1, with accommodation preferences ready for the recommendation demo. */
    private void ensureDemoStudent() {
        String email = "student.asha@example.com";
        UserAccount student = users.findByEmail(email).orElseGet(() -> {
            UserAccount created = users.save(new UserAccount(email, passwordEncoder.encode(demoPassword), Role.STUDENT,
                    "Asha Kulkarni (sample)"));
            StudentProfile profile = studentProfiles.save(new StudentProfile(created));
            colleges.findAllByOrderByNameAsc().stream().findFirst()
                    .ifPresent(vjti -> profile.update(vjti, "MCA", (short) 1, Gender.FEMALE));
            return created;
        });
        studentProfiles.findById(student.getId()).filter(p -> p.getGender() == null)
                .ifPresent(p -> p.update(p.getCollege(), p.getCourse(), p.getYearOfStudy(), Gender.FEMALE));
        if (!studentPreferences.existsById(student.getId())) {
            StudentPreferences prefs = new StudentPreferences(student);
            prefs.update(11_000, 40, TravelMode.ANY, List.of("DOUBLE", "SINGLE"), List.of("WIFI", "MEALS"),
                    List.of("HOT_WATER", "LAUNDRY"));
            studentPreferences.save(prefs);
        }
    }

    private record DemoRoommate(String email, String name, Gender gender, String course, int year, int min, int max,
            List<String> areas, boolean anywhere, SleepSchedule sleep, Smoking smoking, boolean noSmokers,
            StudyEnvironment study, int clean, int guests, FoodPreference food, SharingSize sharing, boolean sameGender,
            String about) {
    }

    /** Invented students with deliberately different habits, so the demo shows strong, partial and excluded matches. */
    private static final List<DemoRoommate> ROOMMATES = List.of(
            new DemoRoommate("student.asha@example.com", null, Gender.FEMALE, null, 1, 8000, 11000, List.of("Matunga", "Dadar"), false,
                    SleepSchedule.EARLY, Smoking.NON_SMOKER, true, StudyEnvironment.QUIET, 4, 2, FoodPreference.VEG,
                    SharingSize.ONE_ROOMMATE, true, "Sample profile: MCA first year, early riser, likes a quiet room."),
            new DemoRoommate("student.meera@example.com", "Meera Joshi (sample)", Gender.FEMALE, "MCA", 1, 8500, 12000,
                    List.of("Matunga", "King's Circle"), false, SleepSchedule.EARLY, Smoking.NON_SMOKER, false,
                    StudyEnvironment.QUIET, 4, 2, FoodPreference.VEG, SharingSize.ONE_ROOMMATE, true,
                    "Sample profile: early mornings, tidy, vegetarian."),
            new DemoRoommate("student.priya@example.com", "Priya Nair (sample)", Gender.FEMALE, "B.Tech Civil", 2, 9000, 13000,
                    List.of("Dadar", "Sion"), false, SleepSchedule.LATE, Smoking.NON_SMOKER, false, StudyEnvironment.LIVELY,
                    2, 4, FoodPreference.NON_VEG, SharingSize.EITHER, false, "Sample profile: night owl, enjoys company."),
            new DemoRoommate("student.kavya@example.com", "Kavya Rao (sample)", Gender.FEMALE, "M.Tech", 1, 7000, 9500,
                    List.of("Wadala"), false, SleepSchedule.REGULAR, Smoking.NON_SMOKER, false, StudyEnvironment.MODERATE,
                    3, 3, FoodPreference.EGGETARIAN, SharingSize.TWO_OR_MORE, false, "Sample profile: budget-conscious, flexible."),
            new DemoRoommate("student.rohan@example.com", "Rohan Mehta (sample)", Gender.MALE, "MCA", 2, 9000, 12000,
                    List.of(), true, SleepSchedule.REGULAR, Smoking.OUTDOOR_SMOKER, false, StudyEnvironment.MODERATE,
                    3, 3, null, SharingSize.EITHER, false, "Sample profile: smokes outdoors only."));

    private void ensureDemoRoommates() {
        for (DemoRoommate d : ROOMMATES) {
            UserAccount student = users.findByEmail(d.email()).orElseGet(() -> {
                UserAccount created = users.save(new UserAccount(d.email(), passwordEncoder.encode(demoPassword), Role.STUDENT, d.name()));
                StudentProfile profile = studentProfiles.save(new StudentProfile(created));
                colleges.findAllByOrderByNameAsc().stream().findFirst()
                        .ifPresent(vjti -> profile.update(vjti, d.course(), (short) d.year(), d.gender()));
                return created;
            });
            if (!roommateProfiles.existsById(student.getId())) {
                RoommateProfile p = new RoommateProfile(student);
                p.update(true, d.min(), d.max(), d.areas(), d.anywhere(), d.sleep(), d.smoking(), d.noSmokers(), d.study(),
                        d.clean(), d.guests(), d.food(), d.sharing(), d.sameGender(), d.about());
                roommateProfiles.save(p);
            }
        }
    }

    /** A clearly labelled placeholder image (no real property photos are used). */
    private static byte[] samplePhoto(String title, String locality, Color colour) {
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(colour);
            g.fillRect(0, 0, 800, 600);
            g.setColor(new Color(255, 255, 255, 60));
            for (int x = 0; x < 800; x += 60) {
                g.fillRect(x, 0, 25, 600); // simple stripes
            }
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 44));
            g.drawString("SAMPLE PHOTO", 60, 120);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 36));
            g.drawString(title, 60, 300);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 30));
            g.drawString(locality, 60, 350);
            g.drawString("Illustration only – not a real property", 60, 520);
        } finally {
            g.dispose();
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

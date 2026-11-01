package com.campusnest.listing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

import com.campusnest.auth.UserAccount;
import com.campusnest.listing.ListingTypes.GenderPolicy;
import com.campusnest.listing.ListingTypes.PropertyType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * An accommodation listing and everything that belongs to it (rooms, charges, amenity answers, photos).
 * Saving the Listing also saves its children (cascade), and removing a child from a list deletes it
 * (orphanRemoval).
 *
 * Mutating methods are package-private on purpose: only services in the listing package can change a
 * listing, and the status can only change through ListingLifecycleService.
 */
@Entity
@Table(name = "listings")
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private UserAccount owner;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", nullable = false, length = 20)
    private PropertyType propertyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender_policy", nullable = false, length = 20)
    private GenderPolicy genderPolicy;

    @Column(name = "address_line", length = 300)
    private String addressLine;

    @Column(length = 100)
    private String locality;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(length = 6)
    private String pincode;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ListingStatus status;

    @Column(name = "status_reason", length = 1000)
    private String statusReason;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "last_confirmed_at")
    private Instant lastConfirmedAt;

    @Column(name = "is_sample_data", nullable = false)
    private boolean sampleData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<RoomOption> rooms = new ArrayList<>();

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ListingCharge> charges = new ArrayList<>();

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ListingAmenity> amenities = new ArrayList<>();

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ListingImage> images = new ArrayList<>();

    protected Listing() {
    }

    Listing(UserAccount owner, String title) {
        this.owner = owner;
        this.title = title.trim();
        this.propertyType = PropertyType.PG;
        this.genderPolicy = GenderPolicy.ANY;
        this.city = "Mumbai";
        this.status = ListingStatus.DRAFT;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /**
     * Replaces the descriptive fields. Returns true if something a reviewer checked has changed
     * (a "material" edit), which sends a published listing back for review.
     */
    boolean updateDetails(String title, String description, PropertyType propertyType, GenderPolicy genderPolicy,
            String addressLine, String locality, String pincode, BigDecimal latitude, BigDecimal longitude) {
        title = clean(title);
        description = clean(description);
        addressLine = clean(addressLine);
        locality = clean(locality);
        pincode = clean(pincode);
        boolean material = !Objects.equals(this.title, title)
                || !Objects.equals(this.description, description)
                || this.propertyType != propertyType
                || this.genderPolicy != genderPolicy
                || !Objects.equals(this.addressLine, addressLine)
                || !Objects.equals(this.locality, locality)
                || !Objects.equals(this.pincode, pincode)
                || !sameNumber(this.latitude, latitude)
                || !sameNumber(this.longitude, longitude);
        this.title = title;
        this.description = description;
        this.propertyType = propertyType;
        this.genderPolicy = genderPolicy;
        this.addressLine = addressLine;
        this.locality = locality;
        this.pincode = pincode;
        this.latitude = latitude;
        this.longitude = longitude;
        touch();
        return material;
    }

    /** Only ListingLifecycleService calls this, after checking the move is allowed. */
    void applyStatus(ListingStatus newStatus, String reason, Instant now) {
        if (newStatus == ListingStatus.PENDING_VERIFICATION) {
            this.submittedAt = now;
        }
        if (newStatus.isPublished() && this.publishedAt == null) {
            this.publishedAt = now;
        }
        this.status = newStatus;
        this.statusReason = reason;
        touch();
    }

    void confirmAvailability(Instant now) {
        this.lastConfirmedAt = now;
        touch();
    }

    void markAsSampleData() {
        this.sampleData = true;
    }

    void touch() {
        this.updatedAt = Instant.now();
    }

    public int totalBedsAvailable() {
        return rooms.stream().mapToInt(RoomOption::getBedsAvailable).sum();
    }

    public OptionalInt minimumRent() {
        return rooms.stream().mapToInt(RoomOption::getMonthlyRent).min();
    }

    private static String clean(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    /** 19.02 and 19.020000 are the same coordinate, but BigDecimal.equals would say they differ. */
    private static boolean sameNumber(BigDecimal a, BigDecimal b) {
        return (a == null && b == null) || (a != null && b != null && a.compareTo(b) == 0);
    }

    // ---- collections: the listing package edits these directly ----

    List<RoomOption> rooms() {
        return rooms;
    }

    List<ListingCharge> charges() {
        return charges;
    }

    List<ListingAmenity> amenityAnswers() {
        return amenities;
    }

    List<ListingImage> imageList() {
        return images;
    }

    // ---- read-only accessors ----

    public Long getId() {
        return id;
    }

    public UserAccount getOwner() {
        return owner;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public PropertyType getPropertyType() {
        return propertyType;
    }

    public GenderPolicy getGenderPolicy() {
        return genderPolicy;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public String getLocality() {
        return locality;
    }

    public String getCity() {
        return city;
    }

    public String getPincode() {
        return pincode;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public ListingStatus getStatus() {
        return status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getLastConfirmedAt() {
        return lastConfirmedAt;
    }

    public boolean isSampleData() {
        return sampleData;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<RoomOption> getRooms() {
        return List.copyOf(rooms);
    }

    public List<ListingCharge> getCharges() {
        return List.copyOf(charges);
    }

    public List<ListingAmenity> getAmenityAnswers() {
        return List.copyOf(amenities);
    }

    public List<ListingImage> getImages() {
        return List.copyOf(images);
    }
}

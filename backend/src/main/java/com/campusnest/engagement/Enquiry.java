package com.campusnest.engagement;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.campusnest.auth.UserAccount;
import com.campusnest.listing.Listing;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * A private conversation between one student and the owner of one listing. Only those two people can read it
 * (not other students, not other owners, and not administrators).
 */
@Entity
@Table(name = "enquiries")
public class Enquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id")
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private UserAccount student;

    @Column(name = "student_shares_contact", nullable = false)
    private boolean studentSharesContact;

    @Column(name = "owner_shares_contact", nullable = false)
    private boolean ownerSharesContact;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_message_at", nullable = false)
    private Instant lastMessageAt;

    @OneToMany(mappedBy = "enquiry", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<EnquiryMessage> messages = new ArrayList<>();

    protected Enquiry() {
    }

    Enquiry(Listing listing, UserAccount student) {
        this.listing = listing;
        this.student = student;
        this.createdAt = Instant.now();
        this.lastMessageAt = this.createdAt;
    }

    void addMessage(UserAccount sender, String body) {
        messages.add(new EnquiryMessage(this, sender, body.trim()));
        lastMessageAt = Instant.now();
    }

    /** Sharing is one-way: once a person shares contact details in a thread, they stay shared there. */
    void studentSharesContact() {
        this.studentSharesContact = true;
    }

    void ownerSharesContact() {
        this.ownerSharesContact = true;
    }

    public boolean involves(Long userId) {
        return student.getId().equals(userId) || listing.getOwner().getId().equals(userId);
    }

    public Long getId() {
        return id;
    }

    public Listing getListing() {
        return listing;
    }

    public UserAccount getStudent() {
        return student;
    }

    public boolean isStudentSharesContact() {
        return studentSharesContact;
    }

    public boolean isOwnerSharesContact() {
        return ownerSharesContact;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }

    public List<EnquiryMessage> getMessages() {
        return List.copyOf(messages);
    }
}

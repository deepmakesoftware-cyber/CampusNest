package com.campusnest.engagement;

import java.time.Instant;

import com.campusnest.auth.UserAccount;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One message in an enquiry thread. Stored as plain text; the frontend displays it as text (never as HTML). */
@Entity
@Table(name = "enquiry_messages")
public class EnquiryMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enquiry_id")
    private Enquiry enquiry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id")
    private UserAccount sender;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EnquiryMessage() {
    }

    EnquiryMessage(Enquiry enquiry, UserAccount sender, String body) {
        this.enquiry = enquiry;
        this.sender = sender;
        this.body = body;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UserAccount getSender() {
        return sender;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

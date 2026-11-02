package com.campusnest.roommate;

import java.time.Instant;

import com.campusnest.auth.UserAccount;
import com.campusnest.roommate.RoommateTypes.RequestStatus;

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
import jakarta.persistence.Table;

/** "Let's connect" from one student to another. Contact details are shared only once it is ACCEPTED. */
@Entity
@Table(name = "roommate_requests")
public class RoommateRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_user_id")
    private UserAccount from;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_user_id")
    private UserAccount to;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    @Column(length = 500)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    protected RoommateRequest() {
    }

    RoommateRequest(UserAccount from, UserAccount to, String message) {
        this.from = from;
        this.to = to;
        this.message = (message == null || message.isBlank()) ? null : message.trim();
        this.status = RequestStatus.PENDING;
        this.createdAt = Instant.now();
    }

    void respond(RequestStatus newStatus) {
        this.status = newStatus;
        this.respondedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UserAccount getFrom() {
        return from;
    }

    public UserAccount getTo() {
        return to;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public boolean isActive() {
        return status == RequestStatus.PENDING || status == RequestStatus.ACCEPTED;
    }

    public boolean involves(Long userId) {
        return from.getId().equals(userId) || to.getId().equals(userId);
    }
}

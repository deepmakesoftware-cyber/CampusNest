package com.campusnest.roommate;

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

@Entity
@Table(name = "roommate_blocks")
public class RoommateBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocker_id")
    private UserAccount blocker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocked_id")
    private UserAccount blocked;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RoommateBlock() {
    }

    RoommateBlock(UserAccount blocker, UserAccount blocked) {
        this.blocker = blocker;
        this.blocked = blocked;
        this.createdAt = Instant.now();
    }

    public UserAccount getBlocker() {
        return blocker;
    }

    public UserAccount getBlocked() {
        return blocked;
    }
}

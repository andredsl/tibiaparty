package com.tibia.app.domain.entity;

import com.tibia.app.domain.enums.MemberRole;
import com.tibia.app.domain.enums.MemberStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "party_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_party_character",
                columnNames = {"party_id", "character_id"}
        ),
        indexes = {
                @Index(name = "idx_member_party", columnList = "party_id"),
                @Index(name = "idx_member_character", columnList = "character_id"),
                @Index(name = "idx_member_status", columnList = "status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private GameCharacter character;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private MemberRole role = MemberRole.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private MemberStatus status = MemberStatus.PENDING;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "rating_given")
    private Integer ratingGiven; // 1-5

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
    }

    public boolean isLeader() {
        return role == MemberRole.LEADER;
    }

    public boolean isConfirmed() {
        return status == MemberStatus.CONFIRMED;
    }

    public boolean isPending() {
        return status == MemberStatus.PENDING;
    }

    public void confirm() {
        this.status = MemberStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = MemberStatus.REJECTED;
    }

    public void leave() {
        this.status = MemberStatus.LEFT;
        this.leftAt = LocalDateTime.now();
    }
}

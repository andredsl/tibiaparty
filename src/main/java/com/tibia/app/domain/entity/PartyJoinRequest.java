package com.tibia.app.domain.entity;

import com.tibia.app.domain.enums.JoinRequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "party_join_requests",
        indexes = {
                @Index(name = "idx_join_request_party", columnList = "party_id"),
                @Index(name = "idx_join_request_character", columnList = "character_id"),
                @Index(name = "idx_join_request_status", columnList = "status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartyJoinRequest {

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
    @Column(nullable = false, length = 20)
    @Builder.Default
    private JoinRequestStatus status = JoinRequestStatus.PENDING;

    @Column(length = 200)
    private String message; // Mensagem do jogador ao solicitar

    @Column(name = "rejection_reason", length = 200)
    private String rejectionReason; // Motivo da recusa pelo lider

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "seen_at")
    private LocalDateTime seenAt; // Quando o usuario viu a notificacao de recusa

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public boolean isPending() {
        return status == JoinRequestStatus.PENDING;
    }

    public boolean isAccepted() {
        return status == JoinRequestStatus.ACCEPTED;
    }

    public boolean isRejected() {
        return status == JoinRequestStatus.REJECTED;
    }

    public void accept() {
        this.status = JoinRequestStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        this.status = JoinRequestStatus.REJECTED;
        this.rejectionReason = reason;
        this.respondedAt = LocalDateTime.now();
    }
}

package com.tibia.app.domain.entity;

import com.tibia.app.domain.enums.BetStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bets",
        indexes = {
                @Index(name = "idx_bet_round", columnList = "betting_round_id"),
                @Index(name = "idx_bet_user", columnList = "user_id"),
                @Index(name = "idx_bet_status", columnList = "status"),
                @Index(name = "idx_bet_creature", columnList = "creature_name")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "betting_round_id", nullable = false)
    private BettingRound bettingRound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private GameCharacter character; // Personagem que fez a aposta

    @Column(name = "creature_name", nullable = false, length = 100)
    private String creatureName; // Nome da criatura apostada

    @Column(name = "creature_image_url")
    private String creatureImageUrl;

    @Column(nullable = false)
    private Long amount; // Quantidade de Tibia Coins apostada

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BetStatus status = BetStatus.PENDING;

    @Column(name = "prize_amount")
    private Long prizeAmount; // Premio ganho (se vencedor)

    @Column(name = "admin_notes", length = 500)
    private String adminNotes; // Notas do admin sobre confirmacao

    @Column(name = "confirmed_by", length = 100)
    private String confirmedBy; // Nome do admin que confirmou

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public boolean isPending() {
        return status == BetStatus.PENDING;
    }

    public boolean isConfirmed() {
        return status == BetStatus.CONFIRMED;
    }

    public boolean isWinner() {
        return status == BetStatus.WON;
    }

    public boolean canBeCancelled() {
        return status == BetStatus.PENDING;
    }

    public void confirm(String adminName) {
        this.status = BetStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.confirmedBy = adminName;
    }

    public void markAsWinner(Long prize) {
        this.status = BetStatus.WON;
        this.prizeAmount = prize;
        this.finalizedAt = LocalDateTime.now();
    }

    public void markAsLoser() {
        this.status = BetStatus.LOST;
        this.finalizedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = BetStatus.CANCELLED;
        this.finalizedAt = LocalDateTime.now();
    }

    public void refund() {
        this.status = BetStatus.REFUNDED;
        this.finalizedAt = LocalDateTime.now();
    }
}

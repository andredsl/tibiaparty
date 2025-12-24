package com.tibia.app.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "betting_rounds",
        indexes = {
                @Index(name = "idx_betting_round_date", columnList = "target_date", unique = true),
                @Index(name = "idx_betting_round_status", columnList = "is_finalized")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BettingRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_date", nullable = false, unique = true)
    private LocalDate targetDate; // Data para qual as apostas sao feitas (dia seguinte)

    @Column(name = "winning_creature")
    private String winningCreature; // Criatura que foi boostada no targetDate

    @Column(name = "winning_creature_image_url")
    private String winningCreatureImageUrl;

    @Column(name = "total_pool")
    @Builder.Default
    private Long totalPool = 0L; // Total de Tibia Coins apostados (confirmados)

    @Column(name = "accumulated_pool")
    @Builder.Default
    private Long accumulatedPool = 0L; // Valor acumulado de rodadas anteriores sem vencedor

    @Column(name = "is_finalized")
    @Builder.Default
    private Boolean isFinalized = false;

    @Column(name = "has_winners")
    @Builder.Default
    private Boolean hasWinners = false;

    @OneToMany(mappedBy = "bettingRound", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Bet> bets = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getTotalPrizePool() {
        return totalPool + accumulatedPool;
    }

    public int getConfirmedBetsCount() {
        return (int) bets.stream()
                .filter(b -> b.getStatus().isConfirmed() || b.getStatus().isFinalized())
                .count();
    }

    public int getPendingBetsCount() {
        return (int) bets.stream()
                .filter(b -> b.getStatus().isPending())
                .count();
    }

    /**
     * Verifica se a rodada esta aberta para visualizacao
     */
    public boolean isOpen() {
        return !isFinalized && LocalDateTime.now().isBefore(getBettingDeadline());
    }

    /**
     * Verifica se a rodada pode aceitar apostas.
     * Apostas podem ser feitas ate 2 AM do dia do targetDate.
     * Exemplo: se targetDate = 25/12, pode apostar ate 25/12 as 02:00
     */
    public boolean canAcceptBets() {
        if (isFinalized) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = getBettingDeadline();
        return now.isBefore(deadline);
    }

    /**
     * Retorna o horario limite para apostas (2 AM do targetDate)
     */
    public LocalDateTime getBettingDeadline() {
        return targetDate.atTime(LocalTime.of(2, 0));
    }
}

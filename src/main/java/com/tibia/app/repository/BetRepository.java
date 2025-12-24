package com.tibia.app.repository;

import com.tibia.app.domain.entity.Bet;
import com.tibia.app.domain.entity.BettingRound;
import com.tibia.app.domain.entity.User;
import com.tibia.app.domain.enums.BetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BetRepository extends JpaRepository<Bet, Long> {

    List<Bet> findByUserOrderByCreatedAtDesc(User user);

    List<Bet> findByBettingRoundOrderByCreatedAtDesc(BettingRound round);

    List<Bet> findByBettingRoundAndStatus(BettingRound round, BetStatus status);

    List<Bet> findByBettingRoundAndCreatureNameIgnoreCase(BettingRound round, String creatureName);

    List<Bet> findByBettingRoundAndCreatureNameIgnoreCaseAndStatus(
            BettingRound round, String creatureName, BetStatus status);

    Optional<Bet> findByUserAndBettingRound(User user, BettingRound round);

    List<Bet> findByUserAndBettingRoundOrderByCreatedAtDesc(User user, BettingRound round);

    boolean existsByUserAndBettingRound(User user, BettingRound round);

    @Query("SELECT SUM(b.amount) FROM Bet b WHERE b.bettingRound = :round AND b.status = :status")
    Long sumAmountByBettingRoundAndStatus(
            @Param("round") BettingRound round,
            @Param("status") BetStatus status);

    @Query("SELECT b FROM Bet b WHERE b.status = :status ORDER BY b.createdAt DESC")
    List<Bet> findByStatusOrderByCreatedAtDesc(@Param("status") BetStatus status);

    @Query("SELECT COUNT(b) FROM Bet b WHERE b.bettingRound = :round AND b.status = :status")
    long countByBettingRoundAndStatus(
            @Param("round") BettingRound round,
            @Param("status") BetStatus status);

    @Query("SELECT b.creatureName, COUNT(b), SUM(b.amount) FROM Bet b " +
            "WHERE b.bettingRound = :round AND b.status = :status " +
            "GROUP BY b.creatureName ORDER BY SUM(b.amount) DESC")
    List<Object[]> findBetStatsByRound(
            @Param("round") BettingRound round,
            @Param("status") BetStatus status);
}

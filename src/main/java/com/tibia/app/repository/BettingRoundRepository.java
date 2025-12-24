package com.tibia.app.repository;

import com.tibia.app.domain.entity.BettingRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BettingRoundRepository extends JpaRepository<BettingRound, Long> {

    Optional<BettingRound> findByTargetDate(LocalDate targetDate);

    @Query("SELECT br FROM BettingRound br WHERE br.targetDate = :date")
    Optional<BettingRound> findByDate(@Param("date") LocalDate date);

    @Query("SELECT br FROM BettingRound br WHERE br.isFinalized = false AND br.targetDate < :date")
    List<BettingRound> findUnfinalizedRoundsBeforeDate(@Param("date") LocalDate date);

    @Query("SELECT br FROM BettingRound br WHERE br.isFinalized = false ORDER BY br.targetDate ASC")
    List<BettingRound> findAllUnfinalized();

    @Query("SELECT br FROM BettingRound br ORDER BY br.targetDate DESC")
    List<BettingRound> findAllOrderByDateDesc();

    @Query("SELECT br FROM BettingRound br WHERE br.isFinalized = true ORDER BY br.targetDate DESC")
    List<BettingRound> findAllFinalizedOrderByDateDesc();

    @Query("SELECT COALESCE(SUM(br.totalPool + br.accumulatedPool), 0) FROM BettingRound br " +
            "WHERE br.isFinalized = false AND br.hasWinners = false")
    Long sumUnclaimedPool();

    /**
     * Soma o total_pool de todas as rodadas finalizadas sem vencedores
     */
    @Query("SELECT COALESCE(SUM(br.totalPool), 0) FROM BettingRound br " +
            "WHERE br.isFinalized = true AND br.hasWinners = false")
    Long sumTotalPoolFromFinalizedRoundsWithoutWinners();

    @Query("SELECT br FROM BettingRound br WHERE br.isFinalized = true AND br.hasWinners = false " +
            "AND br.targetDate < :date ORDER BY br.targetDate DESC")
    List<BettingRound> findFinalizedRoundsWithoutWinnersBeforeDate(@Param("date") LocalDate date);

    @Query("SELECT br FROM BettingRound br WHERE br.targetDate > :date AND br.isFinalized = false " +
            "ORDER BY br.targetDate ASC")
    List<BettingRound> findOpenRoundsAfterDate(@Param("date") LocalDate date);

    default Optional<BettingRound> findCurrentRound() {
        return findByTargetDate(LocalDate.now().plusDays(1));
    }

    default Optional<BettingRound> findTodaysRound() {
        return findByTargetDate(LocalDate.now());
    }
}

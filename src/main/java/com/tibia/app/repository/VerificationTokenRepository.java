package com.tibia.app.repository;

import com.tibia.app.domain.entity.VerificationToken;
import com.tibia.app.domain.enums.VerificationTokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByCode(String code);

    @Query("""
            SELECT t FROM VerificationToken t
            WHERE LOWER(t.characterName) = LOWER(:characterName)
            AND LOWER(t.worldName) = LOWER(:worldName)
            AND t.status = :status
            ORDER BY t.createdAt DESC
            """)
    List<VerificationToken> findByCharacterAndStatus(
            @Param("characterName") String characterName,
            @Param("worldName") String worldName,
            @Param("status") VerificationTokenStatus status);

    @Query("""
            SELECT t FROM VerificationToken t
            WHERE LOWER(t.characterName) = LOWER(:characterName)
            AND LOWER(t.worldName) = LOWER(:worldName)
            AND t.status = 'PENDING'
            ORDER BY t.createdAt DESC
            """)
    Optional<VerificationToken> findPendingByCharacter(
            @Param("characterName") String characterName,
            @Param("worldName") String worldName);

    @Modifying
    @Query("""
            UPDATE VerificationToken t
            SET t.status = 'EXPIRED'
            WHERE LOWER(t.characterName) = LOWER(:characterName)
            AND LOWER(t.worldName) = LOWER(:worldName)
            AND t.status = 'PENDING'
            """)
    int expireAllPendingForCharacter(
            @Param("characterName") String characterName,
            @Param("worldName") String worldName);

    @Modifying
    @Query("""
            UPDATE VerificationToken t
            SET t.status = 'EXPIRED'
            WHERE t.status = 'PENDING'
            AND t.expiresAt < :now
            """)
    int expireOldPendingTokens(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(t) FROM VerificationToken t WHERE t.ipAddress = :ip AND t.createdAt > :since")
    int countRecentByIp(@Param("ip") String ip, @Param("since") LocalDateTime since);

    @Query("""
            SELECT COUNT(t) FROM VerificationToken t
            WHERE LOWER(t.characterName) = LOWER(:characterName)
            AND t.createdAt > :since
            """)
    int countRecentByCharacter(
            @Param("characterName") String characterName,
            @Param("since") LocalDateTime since);

    @Modifying
    @Query("DELETE FROM VerificationToken t WHERE t.status != 'PENDING' AND t.createdAt < :before")
    int deleteOldTokens(@Param("before") LocalDateTime before);
}

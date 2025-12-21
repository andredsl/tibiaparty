package com.tibia.app.repository;

import com.tibia.app.domain.entity.Party;
import com.tibia.app.domain.enums.PartyStatus;
import com.tibia.app.domain.enums.PartyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PartyRepository extends JpaRepository<Party, Long> {

    @Query("""
            SELECT p FROM Party p
            JOIN FETCH p.world w
            JOIN FETCH p.leader l
            WHERE p.id = :id
            """)
    Optional<Party> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT p FROM Party p
            JOIN FETCH p.world w
            JOIN FETCH p.leader l
            WHERE w.id = :worldId
            AND p.type = :type
            AND p.status IN :statuses
            ORDER BY p.createdAt DESC
            """)
    List<Party> findByWorldAndTypeAndStatuses(
            @Param("worldId") Long worldId,
            @Param("type") PartyType type,
            @Param("statuses") List<PartyStatus> statuses);

    @Query("""
            SELECT p FROM Party p
            JOIN FETCH p.world w
            JOIN FETCH p.leader l
            WHERE w.id = :worldId
            AND p.status IN :statuses
            ORDER BY p.createdAt DESC
            """)
    List<Party> findByWorldAndStatuses(
            @Param("worldId") Long worldId,
            @Param("statuses") List<PartyStatus> statuses);

    @Query("""
            SELECT p FROM Party p
            JOIN p.world w
            WHERE LOWER(w.name) = LOWER(:worldName)
            AND p.status IN :statuses
            ORDER BY p.createdAt DESC
            """)
    Page<Party> findByWorldNameAndStatuses(
            @Param("worldName") String worldName,
            @Param("statuses") List<PartyStatus> statuses,
            Pageable pageable);

    @Query("""
            SELECT p FROM Party p
            WHERE p.leader.id = :characterId
            AND p.status IN :statuses
            """)
    List<Party> findByLeaderAndStatuses(
            @Param("characterId") Long characterId,
            @Param("statuses") List<PartyStatus> statuses);

    @Query("""
            SELECT COUNT(p) FROM Party p
            WHERE p.leader.user.id = :userId
            AND p.createdAt > :since
            """)
    int countCreatedByUserSince(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since);

    @Query("""
            SELECT p FROM Party p
            WHERE p.status IN ('FORMING', 'READY')
            AND p.expiresAt < :now
            """)
    List<Party> findExpiredActiveParties(@Param("now") LocalDateTime now);

    @Modifying
    @Query("""
            UPDATE Party p
            SET p.status = 'CANCELLED'
            WHERE p.status IN ('FORMING', 'READY')
            AND p.expiresAt < :now
            """)
    int expireOldParties(@Param("now") LocalDateTime now);

    @Query("""
            SELECT COUNT(p) FROM Party p
            JOIN p.world w
            WHERE w.id = :worldId
            AND p.status IN ('FORMING', 'READY')
            """)
    int countActiveByWorld(@Param("worldId") Long worldId);

    @Query("""
            SELECT COUNT(p) FROM Party p
            WHERE p.leader.id = :characterId
            AND p.type = :type
            AND p.status IN ('FORMING', 'READY', 'IN_PROGRESS')
            """)
    int countActiveByLeaderAndType(
            @Param("characterId") Long characterId,
            @Param("type") PartyType type);

    @Query("""
            SELECT COUNT(p) FROM Party p
            WHERE p.leader.id = :characterId
            AND p.type IN :types
            AND p.status IN ('FORMING', 'READY', 'IN_PROGRESS')
            """)
    int countActiveByLeaderAndTypes(
            @Param("characterId") Long characterId,
            @Param("types") List<PartyType> types);
}

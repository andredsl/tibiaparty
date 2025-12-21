package com.tibia.app.repository;

import com.tibia.app.domain.entity.PartyJoinRequest;
import com.tibia.app.domain.enums.JoinRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PartyJoinRequestRepository extends JpaRepository<PartyJoinRequest, Long> {

    @Query("""
            SELECT r FROM PartyJoinRequest r
            JOIN FETCH r.character c
            JOIN FETCH c.world
            WHERE r.party.id = :partyId
            AND r.status = :status
            ORDER BY r.createdAt ASC
            """)
    List<PartyJoinRequest> findByPartyIdAndStatus(
            @Param("partyId") Long partyId,
            @Param("status") JoinRequestStatus status);

    @Query("""
            SELECT r FROM PartyJoinRequest r
            JOIN FETCH r.party p
            JOIN FETCH p.leader
            WHERE r.character.id = :characterId
            AND r.status = :status
            """)
    List<PartyJoinRequest> findByCharacterIdAndStatus(
            @Param("characterId") Long characterId,
            @Param("status") JoinRequestStatus status);

    @Query("""
            SELECT r FROM PartyJoinRequest r
            WHERE r.party.id = :partyId
            AND r.character.id = :characterId
            AND r.status = 'PENDING'
            """)
    Optional<PartyJoinRequest> findPendingByPartyAndCharacter(
            @Param("partyId") Long partyId,
            @Param("characterId") Long characterId);

    @Query("""
            SELECT COUNT(r) > 0 FROM PartyJoinRequest r
            WHERE r.party.id = :partyId
            AND r.character.id = :characterId
            AND r.status = 'PENDING'
            """)
    boolean existsPendingRequest(
            @Param("partyId") Long partyId,
            @Param("characterId") Long characterId);

    @Query("""
            SELECT r FROM PartyJoinRequest r
            JOIN FETCH r.party p
            JOIN FETCH p.leader l
            WHERE r.character.id = :characterId
            AND r.status = 'REJECTED'
            AND r.respondedAt > :since
            AND r.seenAt IS NULL
            ORDER BY r.respondedAt DESC
            """)
    List<PartyJoinRequest> findRecentRejections(
            @Param("characterId") Long characterId,
            @Param("since") LocalDateTime since);

    @Query("""
            SELECT COUNT(r) FROM PartyJoinRequest r
            WHERE r.party.id = :partyId
            AND r.status = 'PENDING'
            """)
    int countPendingByParty(@Param("partyId") Long partyId);
}

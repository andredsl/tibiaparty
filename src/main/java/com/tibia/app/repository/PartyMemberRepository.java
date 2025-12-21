package com.tibia.app.repository;

import com.tibia.app.domain.entity.Party;
import com.tibia.app.domain.entity.PartyMember;
import com.tibia.app.domain.enums.MemberStatus;
import com.tibia.app.domain.enums.PartyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PartyMemberRepository extends JpaRepository<PartyMember, Long> {

    @Query("""
            SELECT m FROM PartyMember m
            JOIN FETCH m.character c
            JOIN FETCH c.world
            WHERE m.party.id = :partyId
            AND m.status = :status
            ORDER BY m.role DESC, m.joinedAt ASC
            """)
    List<PartyMember> findByPartyIdAndStatus(
            @Param("partyId") Long partyId,
            @Param("status") MemberStatus status);

    @Query("""
            SELECT m FROM PartyMember m
            JOIN FETCH m.character c
            WHERE m.party.id = :partyId
            ORDER BY m.role DESC, m.joinedAt ASC
            """)
    List<PartyMember> findByPartyIdWithCharacter(@Param("partyId") Long partyId);

    Optional<PartyMember> findByPartyIdAndCharacterId(Long partyId, Long characterId);

    @Query("""
            SELECT m FROM PartyMember m
            JOIN m.party p
            WHERE m.character.id = :characterId
            AND p.status IN ('FORMING', 'READY', 'IN_PROGRESS')
            AND m.status = 'CONFIRMED'
            """)
    Optional<PartyMember> findActivePartyMembership(@Param("characterId") Long characterId);

    @Query("""
            SELECT COUNT(m) > 0 FROM PartyMember m
            JOIN m.party p
            WHERE m.character.id = :characterId
            AND p.status IN ('FORMING', 'READY', 'IN_PROGRESS')
            AND m.status = 'CONFIRMED'
            """)
    boolean existsActivePartyMembership(@Param("characterId") Long characterId);

    @Query("""
            SELECT p FROM Party p
            JOIN PartyMember m ON m.party.id = p.id
            WHERE m.character.id = :characterId
            AND p.status IN ('FORMING', 'READY', 'IN_PROGRESS')
            AND m.status = 'CONFIRMED'
            """)
    Optional<Party> findActivePartyByCharacter(@Param("characterId") Long characterId);

    @Query("""
            SELECT m.character.user.id FROM PartyMember m
            WHERE m.party.id = :partyId
            AND m.status = 'CONFIRMED'
            """)
    List<Long> findUserIdsByPartyId(@Param("partyId") Long partyId);

    @Query("""
            SELECT COUNT(m) FROM PartyMember m
            WHERE m.character.user.id = :userId
            AND m.joinedAt > :since
            AND m.status = 'PENDING'
            """)
    int countPendingRequestsByUserSince(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since);

    @Query("""
            SELECT COUNT(m) FROM PartyMember m
            WHERE m.party.id = :partyId
            AND m.status = 'CONFIRMED'
            """)
    int countConfirmedMembers(@Param("partyId") Long partyId);

    boolean existsByPartyIdAndCharacterId(Long partyId, Long characterId);

    @Query("""
            SELECT COUNT(m) > 0 FROM PartyMember m
            JOIN m.party p
            WHERE m.character.id = :characterId
            AND p.type IN :types
            AND p.status IN ('FORMING', 'READY', 'IN_PROGRESS')
            AND m.status = 'CONFIRMED'
            """)
    boolean existsActivePartyMembershipByTypes(
            @Param("characterId") Long characterId,
            @Param("types") List<PartyType> types);
}

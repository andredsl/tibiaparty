package com.tibia.app.repository;

import com.tibia.app.domain.entity.PartyVocationSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartyVocationSlotRepository extends JpaRepository<PartyVocationSlot, Long> {

    List<PartyVocationSlot> findByPartyId(Long partyId);
}

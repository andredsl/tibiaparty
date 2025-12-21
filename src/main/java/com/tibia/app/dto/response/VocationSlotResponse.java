package com.tibia.app.dto.response;

import com.tibia.app.domain.entity.PartyVocationSlot;
import com.tibia.app.domain.enums.Vocation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocationSlotResponse {

    private Vocation vocation;
    private String abbreviation;
    private int slotsNeeded;
    private int slotsFilled;
    private int availableSlots;
    private boolean full;

    public static VocationSlotResponse fromEntity(PartyVocationSlot slot) {
        return VocationSlotResponse.builder()
                .vocation(slot.getVocation())
                .abbreviation(slot.getVocation().getAbbreviation())
                .slotsNeeded(slot.getSlotsNeeded())
                .slotsFilled(slot.getSlotsFilled())
                .availableSlots(slot.getAvailableSlots())
                .full(slot.isFull())
                .build();
    }
}

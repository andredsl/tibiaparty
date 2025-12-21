package com.tibia.app.domain.entity;

import com.tibia.app.domain.enums.Vocation;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "party_vocation_slots")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartyVocationSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Vocation vocation;

    @Column(name = "slots_needed", nullable = false)
    private Integer slotsNeeded;

    @Column(name = "slots_filled", nullable = false)
    @Builder.Default
    private Integer slotsFilled = 0;

    public int getAvailableSlots() {
        return Math.max(0, slotsNeeded - slotsFilled);
    }

    public boolean isFull() {
        return slotsFilled >= slotsNeeded;
    }

    public boolean hasSlotAvailable() {
        return slotsFilled < slotsNeeded;
    }

    public void incrementFilled() {
        this.slotsFilled++;
    }

    public void decrementFilled() {
        if (this.slotsFilled > 0) {
            this.slotsFilled--;
        }
    }
}

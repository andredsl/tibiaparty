package com.tibia.app.dto.response;

import com.tibia.app.domain.entity.Party;
import com.tibia.app.domain.enums.PartyStatus;
import com.tibia.app.domain.enums.PartyType;
import com.tibia.app.domain.enums.Vocation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartyResponse {

    private Long id;
    private PartyType type;
    private PartyStatus status;
    private String displayName;
    private String world;
    private Long leaderId;
    private String leaderName;
    private String leaderVocation;
    private int leaderLevel;
    private String levelRange;
    private Integer levelMin;
    private Integer levelMax;
    private int currentMembers;
    private int maxMembers;
    private int availableSlots;
    private String description;
    private LocalDateTime scheduledTime;
    private boolean startWhenFull;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private long minutesUntilExpiration;

    // Soul Core
    private Long pricePerPlayer;
    private boolean leaderParticipates;

    // Vocation slots
    private boolean hasVocationRequirements;
    private List<VocationSlotResponse> vocationSlots;

    public String getFormattedPrice() {
        if (pricePerPlayer == null || pricePerPlayer == 0) return "Gratis";
        if (pricePerPlayer >= 1_000_000_000) {
            return String.format("%.1fkk", pricePerPlayer / 1_000_000.0);
        } else if (pricePerPlayer >= 1_000_000) {
            return String.format("%.1fkk", pricePerPlayer / 1_000_000.0);
        } else if (pricePerPlayer >= 1_000) {
            return String.format("%dk", pricePerPlayer / 1_000);
        }
        return pricePerPlayer + " gp";
    }

    public static PartyResponse fromEntity(Party party) {
        List<VocationSlotResponse> slots = new ArrayList<>();
        if (party.getVocationSlots() != null) {
            slots = party.getVocationSlots().stream()
                    .map(VocationSlotResponse::fromEntity)
                    .collect(Collectors.toList());
        }

        return PartyResponse.builder()
                .id(party.getId())
                .type(party.getType())
                .status(party.getStatus())
                .displayName(party.getDisplayName())
                .world(party.getWorld().getName())
                .leaderId(party.getLeader().getId())
                .leaderName(party.getLeader().getName())
                .leaderVocation(party.getLeader().getVocationAbbreviation())
                .leaderLevel(party.getLeader().getLevel())
                .levelRange(party.getLevelRange())
                .levelMin(party.getLevelMin())
                .levelMax(party.getLevelMax())
                .currentMembers(party.getConfirmedMembersCount())
                .maxMembers(party.getMaxMembers())
                .availableSlots(party.getAvailableSlots())
                .description(party.getDescription())
                .scheduledTime(party.getScheduledTime())
                .startWhenFull(party.isStartWhenFull())
                .createdAt(party.getCreatedAt())
                .expiresAt(party.getExpiresAt())
                .minutesUntilExpiration(party.isExpired() ? 0 :
                        Duration.between(LocalDateTime.now(), party.getExpiresAt()).toMinutes())
                .pricePerPlayer(party.getPricePerPlayer())
                .leaderParticipates(party.isLeaderParticipating())
                .hasVocationRequirements(party.hasVocationRequirements())
                .vocationSlots(slots)
                .build();
    }

    public boolean canJoin(Vocation playerVocation) {
        if (availableSlots <= 0) return false;
        if (!hasVocationRequirements) return true;

        // Verifica se ha vaga para a vocacao do player
        return vocationSlots.stream()
                .filter(slot -> isSameVocationClass(slot.getVocation(), playerVocation))
                .anyMatch(slot -> !slot.isFull());
    }

    public String getJoinBlockReason(Vocation playerVocation) {
        if (availableSlots <= 0) {
            return "PT lotada";
        }
        if (hasVocationRequirements) {
            boolean hasSlot = vocationSlots.stream()
                    .anyMatch(slot -> isSameVocationClass(slot.getVocation(), playerVocation));

            if (!hasSlot) {
                return "PT nao procura " + playerVocation.getAbbreviation();
            }

            boolean slotFull = vocationSlots.stream()
                    .filter(slot -> isSameVocationClass(slot.getVocation(), playerVocation))
                    .allMatch(VocationSlotResponse::isFull);

            if (slotFull) {
                return "Vagas para " + playerVocation.getAbbreviation() + " ja preenchidas";
            }
        }
        return null;
    }

    private boolean isSameVocationClass(Vocation v1, Vocation v2) {
        return getBaseVocation(v1).equals(getBaseVocation(v2));
    }

    private String getBaseVocation(Vocation vocation) {
        return switch (vocation) {
            case ELITE_KNIGHT, KNIGHT -> "KNIGHT";
            case ROYAL_PALADIN, PALADIN -> "PALADIN";
            case ELDER_DRUID, DRUID -> "DRUID";
            case MASTER_SORCERER, SORCERER -> "SORCERER";
            default -> "NONE";
        };
    }
}

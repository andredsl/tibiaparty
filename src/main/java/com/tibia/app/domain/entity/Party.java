package com.tibia.app.domain.entity;

import com.tibia.app.domain.enums.MemberStatus;
import com.tibia.app.domain.enums.PartyStatus;
import com.tibia.app.domain.enums.PartyType;
import com.tibia.app.domain.enums.Vocation;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "parties",
        indexes = {
                @Index(name = "idx_party_world_status", columnList = "world_id, status"),
                @Index(name = "idx_party_world_type_status", columnList = "world_id, type, status"),
                @Index(name = "idx_party_leader", columnList = "leader_id"),
                @Index(name = "idx_party_expires", columnList = "expires_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Party {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PartyType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "world_id", nullable = false)
    private World world;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id", nullable = false)
    private GameCharacter leader;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PartyStatus status = PartyStatus.FORMING;

    @Column(length = 100)
    private String location; // Para Hunt: "Asuras", "Ferumbras Seal", etc.

    @Column(name = "boss_name", length = 100)
    private String bossName; // Para Boss: "Ferumbras", "Soulwar", etc.

    @Column(name = "soul_core_name", length = 100)
    private String soulCoreName; // Para Soul Core: nome da criatura

    @Column(name = "price_per_player")
    private Long pricePerPlayer; // Preco por player em gold (para Soul Core)

    @Column(name = "leader_participates")
    @Builder.Default
    private Boolean leaderParticipates = true; // Se o lider participa ou apenas gerencia

    @Column(name = "level_min")
    private Integer levelMin;

    @Column(name = "level_max")
    private Integer levelMax;

    @Column(name = "max_members")
    @Builder.Default
    private Integer maxMembers = 5;

    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime; // Para Boss com horário marcado

    @Column(name = "start_when_full")
    @Builder.Default
    private Boolean startWhenFull = false; // Iniciar assim que a PT estiver completa

    @Column(length = 500)
    private String description;

    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<PartyVocationSlot> vocationSlots = new ArrayList<>();

    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PartyMember> members = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusHours(2);
        }
    }

    public boolean isActive() {
        return status.isActive();
    }

    public boolean isFull() {
        return getConfirmedMembersCount() >= maxMembers;
    }

    public int getConfirmedMembersCount() {
        return (int) members.stream()
                .filter(m -> m.getStatus() == MemberStatus.CONFIRMED)
                .count();
    }

    public int getAvailableSlots() {
        return maxMembers - getConfirmedMembersCount();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public String getDisplayName() {
        return switch (type) {
            case BOSS -> bossName;
            case SOUL_CORE -> soulCoreName;
            default -> location;
        };
    }

    public boolean isLeaderParticipating() {
        return leaderParticipates == null || leaderParticipates;
    }

    public boolean isStartWhenFull() {
        return startWhenFull != null && startWhenFull;
    }

    public boolean hasScheduledTime() {
        return scheduledTime != null && !isStartWhenFull();
    }

    public String getLevelRange() {
        if (levelMin == null && levelMax == null) return "Qualquer";
        if (levelMin != null && levelMax != null) return levelMin + " - " + levelMax;
        if (levelMin != null) return levelMin + "+";
        return "Ate " + levelMax;
    }

    public boolean hasVocationRequirements() {
        return vocationSlots != null && !vocationSlots.isEmpty();
    }

    public Optional<PartyVocationSlot> getSlotForVocation(Vocation vocation) {
        if (vocationSlots == null) return Optional.empty();

        // Mapeia vocacoes base (Knight -> EK, Paladin -> RP, etc)
        Vocation baseVocation = mapToBaseVocation(vocation);

        return vocationSlots.stream()
                .filter(slot -> mapToBaseVocation(slot.getVocation()) == baseVocation)
                .findFirst();
    }

    public boolean hasSlotAvailableForVocation(Vocation vocation) {
        if (!hasVocationRequirements()) {
            // Se nao ha requisitos de vocacao, qualquer um pode entrar
            return !isFull();
        }

        Optional<PartyVocationSlot> slot = getSlotForVocation(vocation);
        return slot.map(PartyVocationSlot::hasSlotAvailable).orElse(false);
    }

    public int getAvailableSlotsForVocation(Vocation vocation) {
        return getSlotForVocation(vocation)
                .map(PartyVocationSlot::getAvailableSlots)
                .orElse(0);
    }

    public int getNeededForVocation(Vocation vocation) {
        return getSlotForVocation(vocation)
                .map(PartyVocationSlot::getSlotsNeeded)
                .orElse(0);
    }

    public int getFilledForVocation(Vocation vocation) {
        return getSlotForVocation(vocation)
                .map(PartyVocationSlot::getSlotsFilled)
                .orElse(0);
    }

    private Vocation mapToBaseVocation(Vocation vocation) {
        return switch (vocation) {
            case ELITE_KNIGHT, KNIGHT -> Vocation.ELITE_KNIGHT;
            case ROYAL_PALADIN, PALADIN -> Vocation.ROYAL_PALADIN;
            case ELDER_DRUID, DRUID -> Vocation.ELDER_DRUID;
            case MASTER_SORCERER, SORCERER -> Vocation.MASTER_SORCERER;
            default -> vocation;
        };
    }

    public void addVocationSlot(PartyVocationSlot slot) {
        if (vocationSlots == null) {
            vocationSlots = new ArrayList<>();
        }
        slot.setParty(this);
        vocationSlots.add(slot);
    }
}

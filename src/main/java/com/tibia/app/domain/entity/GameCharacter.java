package com.tibia.app.domain.entity;

import com.tibia.app.domain.enums.Vocation;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "characters",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_character_name_world",
                columnNames = {"name", "world_id"}
        ),
        indexes = {
                @Index(name = "idx_character_user", columnList = "user_id"),
                @Index(name = "idx_character_world", columnList = "world_id"),
                @Index(name = "idx_character_name", columnList = "name")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "world_id", nullable = false)
    private World world;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private Integer level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Vocation vocation;

    @Column(name = "is_main", nullable = false)
    @Builder.Default
    private boolean main = false;

    @Column(name = "verified_at", nullable = false)
    private LocalDateTime verifiedAt;

    @Column(name = "tibia_account_age_days")
    private Integer tibiaAccountAgeDays;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        verifiedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getVocationAbbreviation() {
        return vocation != null ? vocation.getAbbreviation() : "?";
    }

    public boolean needsReverification(int maxDays) {
        if (verifiedAt == null) return true;
        return verifiedAt.plusDays(maxDays).isBefore(LocalDateTime.now());
    }
}

package com.tibia.app.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "worlds")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class World {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = false, length = 20)
    private String location; // SA, EU, NA

    @Column(name = "pvp_type", nullable = false, length = 30)
    private String pvpType; // Open PvP, Optional PvP, Hardcore PvP, Retro Open PvP, Retro Hardcore PvP

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public boolean isBrazilian() {
        return "SA".equalsIgnoreCase(location);
    }
}

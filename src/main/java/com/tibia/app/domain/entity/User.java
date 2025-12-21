package com.tibia.app.domain.entity;

import com.tibia.app.domain.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal reputation = new BigDecimal("5.00");

    @Column(name = "total_hunts")
    @Builder.Default
    private Integer totalHunts = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GameCharacter> characters = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public GameCharacter getMainCharacter() {
        return characters.stream()
                .filter(GameCharacter::isMain)
                .findFirst()
                .orElse(characters.isEmpty() ? null : characters.get(0));
    }

    public void addCharacter(GameCharacter character) {
        characters.add(character);
        character.setUser(this);
    }

    public boolean isBlocked() {
        return status == UserStatus.BLOCKED;
    }

    public boolean hasPassword() {
        return passwordHash != null && !passwordHash.isBlank();
    }
}

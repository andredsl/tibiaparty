package com.tibia.app.domain.entity;

import com.tibia.app.domain.enums.VerificationTokenStatus;
import com.tibia.app.domain.enums.VerificationTokenType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification_tokens",
        indexes = {
                @Index(name = "idx_token_code", columnList = "code"),
                @Index(name = "idx_token_char_world", columnList = "character_name, world_name"),
                @Index(name = "idx_token_expires", columnList = "expires_at"),
                @Index(name = "idx_token_status", columnList = "status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationToken {

    private static final int DEFAULT_EXPIRATION_MINUTES = 15;
    private static final int MAX_ATTEMPTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "character_name", nullable = false, length = 50)
    private String characterName;

    @Column(name = "world_name", nullable = false, length = 50)
    private String worldName;

    @Column(nullable = false, unique = true, length = 12)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationTokenType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VerificationTokenStatus status = VerificationTokenStatus.PENDING;

    @Column(nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusMinutes(DEFAULT_EXPIRATION_MINUTES);
        }
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isUsed() {
        return status == VerificationTokenStatus.USED;
    }

    public boolean isPending() {
        return status == VerificationTokenStatus.PENDING;
    }

    public boolean isValid() {
        return !isExpired() && isPending() && attempts < MAX_ATTEMPTS;
    }

    public void incrementAttempts() {
        this.attempts++;
    }

    public int getRemainingAttempts() {
        return Math.max(0, MAX_ATTEMPTS - attempts);
    }

    public void markAsUsed() {
        this.status = VerificationTokenStatus.USED;
        this.usedAt = LocalDateTime.now();
    }

    public void markAsExpired() {
        this.status = VerificationTokenStatus.EXPIRED;
    }

    public long getMinutesUntilExpiration() {
        if (isExpired()) return 0;
        return java.time.Duration.between(LocalDateTime.now(), expiresAt).toMinutes();
    }
}

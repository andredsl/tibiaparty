package com.tibia.app.dto.response;

import com.tibia.app.domain.entity.GameCharacter;
import com.tibia.app.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResult {

    private boolean success;
    private String message;
    private User user;
    private GameCharacter character;
    private int attemptsRemaining;
    private boolean suggestWait;
    private int suggestedWaitSeconds;

    public static VerificationResult success(User user, GameCharacter character) {
        return VerificationResult.builder()
                .success(true)
                .message("Verificação concluída com sucesso!")
                .user(user)
                .character(character)
                .build();
    }

    public static VerificationResult failure(String message, int attemptsRemaining) {
        return VerificationResult.builder()
                .success(false)
                .message(message)
                .attemptsRemaining(attemptsRemaining)
                .build();
    }

    public static VerificationResult cacheDelay(int attemptsRemaining) {
        return VerificationResult.builder()
                .success(false)
                .message("Código não encontrado. O Tibia.com pode demorar para atualizar.")
                .attemptsRemaining(attemptsRemaining)
                .suggestWait(true)
                .suggestedWaitSeconds(120)
                .build();
    }
}

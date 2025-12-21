package com.tibia.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationTokenResponse {

    private String code;
    private String characterName;
    private String world;
    private Integer level;
    private String vocation;
    private LocalDateTime expiresAt;
    private long minutesUntilExpiration;
    private String instructions;

    public static VerificationTokenResponseBuilder builderWithInstructions(String code) {
        String instructions = String.format("""
                1. Acesse tibia.com e faça login na sua conta
                2. Vá em "My Account" → "Characters"
                3. Clique em "Edit" no character informado
                4. No campo "Comment", adicione o código: %s
                5. Salve as alterações e volte aqui para verificar

                ⚠️ O código expira em 15 minutos.
                ⚠️ O Tibia.com pode demorar até 2 minutos para atualizar.
                """, code);

        return VerificationTokenResponse.builder()
                .code(code)
                .instructions(instructions);
    }
}

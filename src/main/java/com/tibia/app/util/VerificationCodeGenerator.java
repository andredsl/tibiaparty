package com.tibia.app.util;

import com.tibia.app.domain.enums.VerificationTokenType;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class VerificationCodeGenerator {

    // Caracteres que não confundem: sem 0, O, I, l, 1
    private static final String CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 5;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PREFIX = "TPF-";

    /**
     * Gera código único no formato TPF-RXXXXX (R=Register, L=Login, C=AddChar)
     */
    public String generate(VerificationTokenType type) {
        StringBuilder code = new StringBuilder(PREFIX);
        code.append(type.getPrefix());

        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = RANDOM.nextInt(CHARSET.length());
            code.append(CHARSET.charAt(index));
        }

        return code.toString();
    }

    /**
     * Valida formato do código
     */
    public boolean isValidFormat(String code) {
        if (code == null || code.length() != 10) {
            return false;
        }
        return code.matches("TPF-[RLC][A-HJ-NP-Z2-9]{5}");
    }

    /**
     * Extrai o tipo do token a partir do código
     */
    public VerificationTokenType extractType(String code) {
        if (!isValidFormat(code)) {
            return null;
        }

        char typeChar = code.charAt(4);
        return switch (typeChar) {
            case 'R' -> VerificationTokenType.REGISTER;
            case 'L' -> VerificationTokenType.LOGIN;
            case 'C' -> VerificationTokenType.ADD_CHAR;
            default -> null;
        };
    }
}

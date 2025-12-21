package com.tibia.app.domain.enums;

public enum VerificationTokenType {
    REGISTER("R"),    // Primeiro acesso
    LOGIN("L"),       // Login recorrente
    ADD_CHAR("C");    // Adicionar character secundário

    private final String prefix;

    VerificationTokenType(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}

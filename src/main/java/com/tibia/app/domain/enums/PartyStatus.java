package com.tibia.app.domain.enums;

public enum PartyStatus {
    FORMING("Formando"),
    READY("Pronta"),
    IN_PROGRESS("Em Hunt"),
    FINISHED("Encerrada"),
    CANCELLED("Cancelada");

    private final String displayName;

    PartyStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return this == FORMING || this == READY || this == IN_PROGRESS;
    }
}

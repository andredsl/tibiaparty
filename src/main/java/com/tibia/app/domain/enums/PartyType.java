package com.tibia.app.domain.enums;

public enum PartyType {
    HUNT("Hunt"),
    BOSS("Boss"),
    SOUL_CORE("Soul Core");

    private final String displayName;

    PartyType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

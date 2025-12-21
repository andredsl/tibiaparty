package com.tibia.app.domain.enums;

public enum Vocation {
    ELITE_KNIGHT("EK", "Elite Knight"),
    ROYAL_PALADIN("RP", "Royal Paladin"),
    ELDER_DRUID("ED", "Elder Druid"),
    MASTER_SORCERER("MS", "Master Sorcerer"),
    KNIGHT("K", "Knight"),
    PALADIN("P", "Paladin"),
    DRUID("D", "Druid"),
    SORCERER("S", "Sorcerer"),
    NONE("None", "None");

    private final String abbreviation;
    private final String fullName;

    Vocation(String abbreviation, String fullName) {
        this.abbreviation = abbreviation;
        this.fullName = fullName;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public String getFullName() {
        return fullName;
    }

    public boolean isPromoted() {
        return this == ELITE_KNIGHT || this == ROYAL_PALADIN ||
               this == ELDER_DRUID || this == MASTER_SORCERER;
    }

    public String getBaseVocation() {
        return switch (this) {
            case ELITE_KNIGHT, KNIGHT -> "Knight";
            case ROYAL_PALADIN, PALADIN -> "Paladin";
            case ELDER_DRUID, DRUID -> "Druid";
            case MASTER_SORCERER, SORCERER -> "Sorcerer";
            default -> "None";
        };
    }

    public static Vocation fromTibiaName(String name) {
        if (name == null) return NONE;

        return switch (name.toLowerCase().trim()) {
            case "elite knight" -> ELITE_KNIGHT;
            case "royal paladin" -> ROYAL_PALADIN;
            case "elder druid" -> ELDER_DRUID;
            case "master sorcerer" -> MASTER_SORCERER;
            case "knight" -> KNIGHT;
            case "paladin" -> PALADIN;
            case "druid" -> DRUID;
            case "sorcerer" -> SORCERER;
            default -> NONE;
        };
    }
}

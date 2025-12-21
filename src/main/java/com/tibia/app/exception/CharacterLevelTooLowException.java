package com.tibia.app.exception;

public class CharacterLevelTooLowException extends RuntimeException {

    private final int actualLevel;
    private final int minLevel;

    public CharacterLevelTooLowException(int actualLevel, int minLevel) {
        super(String.format("Level mínimo é %d. Seu character tem level %d.", minLevel, actualLevel));
        this.actualLevel = actualLevel;
        this.minLevel = minLevel;
    }

    public int getActualLevel() {
        return actualLevel;
    }

    public int getMinLevel() {
        return minLevel;
    }
}

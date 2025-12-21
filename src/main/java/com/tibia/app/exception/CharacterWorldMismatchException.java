package com.tibia.app.exception;

public class CharacterWorldMismatchException extends RuntimeException {

    private final String characterName;
    private final String expectedWorld;
    private final String actualWorld;

    public CharacterWorldMismatchException(String characterName, String expectedWorld, String actualWorld) {
        super(String.format("Character '%s' pertence ao servidor %s, não %s",
                characterName, actualWorld, expectedWorld));
        this.characterName = characterName;
        this.expectedWorld = expectedWorld;
        this.actualWorld = actualWorld;
    }

    public String getCharacterName() {
        return characterName;
    }

    public String getExpectedWorld() {
        return expectedWorld;
    }

    public String getActualWorld() {
        return actualWorld;
    }
}

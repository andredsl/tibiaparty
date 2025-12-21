package com.tibia.app.exception;

public class CharacterNotFoundException extends RuntimeException {

    private final String characterName;

    public CharacterNotFoundException(String characterName) {
        super("Character não encontrado: " + characterName);
        this.characterName = characterName;
    }

    public String getCharacterName() {
        return characterName;
    }
}

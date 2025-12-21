package com.tibia.app.exception;

public class CharacterAlreadyOwnedException extends RuntimeException {

    private final String characterName;

    public CharacterAlreadyOwnedException(String characterName) {
        super("Character já está vinculado a outro usuário: " + characterName);
        this.characterName = characterName;
    }

    public String getCharacterName() {
        return characterName;
    }
}

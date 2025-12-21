package com.tibia.app.exception;

public class WorldNotFoundException extends RuntimeException {

    private final String worldName;

    public WorldNotFoundException(String worldName) {
        super("Servidor não encontrado: " + worldName);
        this.worldName = worldName;
    }

    public String getWorldName() {
        return worldName;
    }
}

package com.tibia.app.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Personagem ou senha inválidos");
    }
}

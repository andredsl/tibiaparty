package com.tibia.app.exception;

public class PasswordMismatchException extends RuntimeException {

    public PasswordMismatchException() {
        super("As senhas não coincidem");
    }
}

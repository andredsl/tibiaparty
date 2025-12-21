package com.tibia.app.exception;

public class TooManyVerificationAttemptsException extends VerificationException {

    public TooManyVerificationAttemptsException() {
        super("Muitas tentativas de verificação. Por favor, gere um novo código.");
    }
}

package com.tibia.app.exception;

public class VerificationTokenExpiredException extends VerificationException {

    public VerificationTokenExpiredException() {
        super("Código de verificação expirado. Por favor, gere um novo código.");
    }
}

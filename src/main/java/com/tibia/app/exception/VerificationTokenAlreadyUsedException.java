package com.tibia.app.exception;

public class VerificationTokenAlreadyUsedException extends VerificationException {

    public VerificationTokenAlreadyUsedException() {
        super("Este código de verificação já foi utilizado.");
    }
}

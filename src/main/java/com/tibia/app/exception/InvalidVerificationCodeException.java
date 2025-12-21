package com.tibia.app.exception;

public class InvalidVerificationCodeException extends VerificationException {

    public InvalidVerificationCodeException(String message) {
        super(message);
    }

    public InvalidVerificationCodeException() {
        super("Código de verificação inválido");
    }
}

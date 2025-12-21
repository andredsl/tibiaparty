package com.tibia.app.exception;

public class NotAuthenticatedException extends RuntimeException {

    public NotAuthenticatedException() {
        super("Usuário não está autenticado");
    }
}

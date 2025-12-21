package com.tibia.app.exception;

public class UserBlockedException extends RuntimeException {

    public UserBlockedException() {
        super("Esta conta está bloqueada");
    }
}

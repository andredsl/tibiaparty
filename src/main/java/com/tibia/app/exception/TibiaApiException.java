package com.tibia.app.exception;

public class TibiaApiException extends RuntimeException {

    public TibiaApiException(String message) {
        super(message);
    }

    public TibiaApiException(String message, Throwable cause) {
        super(message, cause);
    }
}

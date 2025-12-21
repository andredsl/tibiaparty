package com.tibia.app.exception;

public class RateLimitExceededException extends RuntimeException {

    private final int retryAfterSeconds;

    public RateLimitExceededException(int retryAfterSeconds) {
        super(String.format("Muitas solicitações. Aguarde %d segundos.", retryAfterSeconds));
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}

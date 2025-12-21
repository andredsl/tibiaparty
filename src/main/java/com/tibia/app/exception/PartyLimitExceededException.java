package com.tibia.app.exception;

public class PartyLimitExceededException extends RuntimeException {

    public PartyLimitExceededException(int limit) {
        super("Voce atingiu o limite de " + limit + " parties ativas em 24 horas");
    }
}

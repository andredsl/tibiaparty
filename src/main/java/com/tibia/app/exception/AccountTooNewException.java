package com.tibia.app.exception;

public class AccountTooNewException extends RuntimeException {

    private final int actualDays;
    private final int minDays;

    public AccountTooNewException(int actualDays, int minDays) {
        super(String.format("Conta Tibia deve ter pelo menos %d dias. Sua conta tem aproximadamente %d dias.",
                minDays, actualDays));
        this.actualDays = actualDays;
        this.minDays = minDays;
    }

    public int getActualDays() {
        return actualDays;
    }

    public int getMinDays() {
        return minDays;
    }
}

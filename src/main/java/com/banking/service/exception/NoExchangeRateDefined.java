package com.banking.service.exception;

public class NoExchangeRateDefined extends RuntimeException {
    public NoExchangeRateDefined(String message) {
        super(message);
    }
}

package com.banking.service.exception;

/**
 * Thrown when no exchange rate is configured for a requested currency pair.
 */
public class NoExchangeRateDefined extends RuntimeException {

    /**
     * @param message describes the missing currency pair
     */
    public NoExchangeRateDefined(String message) {
        super(message);
    }
}

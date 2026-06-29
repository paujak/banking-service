package com.banking.service.exception;

/**
 * Thrown when a currency exchange request specifies the same account as both source and destination.
 */
public class CurrencyExchangeWithinSameAccountException extends RuntimeException {

    /**
     * @param message description of the invalid request
     */
    public CurrencyExchangeWithinSameAccountException(String message) {
        super(message);
    }
}

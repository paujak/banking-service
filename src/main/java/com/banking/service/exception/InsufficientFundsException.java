package com.banking.service.exception;

/**
 * Thrown when an account has insufficient balance to complete a withdrawal or currency exchange.
 */
public class InsufficientFundsException extends RuntimeException {

    /**
     * @param message description of the shortfall
     */
    public InsufficientFundsException(String message) {
        super(message);
    }
}

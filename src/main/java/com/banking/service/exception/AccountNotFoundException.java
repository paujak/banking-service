package com.banking.service.exception;

/**
 * Thrown when a requested account cannot be found.
 */
public class AccountNotFoundException extends RuntimeException {

    /**
     * @param message description identifying the missing account
     */
    public AccountNotFoundException(String message) {
        super(message);
    }
}

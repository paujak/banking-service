package com.banking.service.exception;

/**
 * Thrown when a requested user cannot be found.
 */
public class UserIsNotFoundException extends RuntimeException {

    /**
     * @param message description identifying the missing user
     */
    public UserIsNotFoundException(String message) {
        super(message);
    }
}

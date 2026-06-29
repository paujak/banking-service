package com.banking.service.exception;

/**
 * Thrown when an account is modified concurrently and all optimistic-locking retries are exhausted.
 */
public class AccountConcurrentModificationException extends RuntimeException {

    /**
     * @param message description of the conflict
     */
    public AccountConcurrentModificationException(String message) {
        super(message);
    }
}

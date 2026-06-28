package com.banking.service.exception;

public class AccountConcurrentModificationException extends RuntimeException {
    public AccountConcurrentModificationException(String message) {
        super(message);
    }
}

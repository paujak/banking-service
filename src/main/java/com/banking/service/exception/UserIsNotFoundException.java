package com.banking.service.exception;

public class UserIsNotFoundException extends RuntimeException {
    public UserIsNotFoundException(String message) {
        super(message);
    }
}

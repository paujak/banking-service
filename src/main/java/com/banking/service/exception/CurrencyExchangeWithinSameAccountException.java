package com.banking.service.exception;

public class CurrencyExchangeWithinSameAccountException extends RuntimeException {
    public CurrencyExchangeWithinSameAccountException(String message) {
        super(message);
    }
}

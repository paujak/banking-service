package com.banking.service.exception;

/**
 * Thrown when the external debit-check service returns an error or is unreachable.
 */
public class ExternalServiceException extends RuntimeException {

    /**
     * @param message description of the error
     */
    public ExternalServiceException(String message) {
        super(message);
    }
}

package com.banking.service.constant;

/**
 * Classifies the direction and nature of a financial transaction.
 */
public enum TransactionType {
    /** Money added to an account directly. */
    DEPOSIT,
    /** Money removed from an account directly. */
    WITHDRAWAL,
    /** Credit leg of a currency exchange — funds arrive in the destination account. */
    EXCHANGE_IN,
    /** Debit leg of a currency exchange — funds leave the source account. */
    EXCHANGE_OUT,
}

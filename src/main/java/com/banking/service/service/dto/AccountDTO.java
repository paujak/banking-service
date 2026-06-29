package com.banking.service.service.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service-layer representation of an account.
 *
 * @param id            account UUID
 * @param accountNumber unique account identifier
 * @param accountName   human-readable label
 * @param balance       current balance
 * @param currencyCode  ISO 4217 currency code
 */
public record AccountDTO(
        UUID id,
        String accountNumber,
        String accountName,
        BigDecimal balance,
        String currencyCode
) {
}

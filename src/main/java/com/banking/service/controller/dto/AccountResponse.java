package com.banking.service.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * API response payload representing an account.
 * {@code null} fields are excluded from the JSON output.
 *
 * @param id            account UUID
 * @param accountNumber unique account identifier
 * @param accountName   human-readable label
 * @param balance       current balance
 * @param currencyCode  ISO 4217 currency code
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccountResponse(
        UUID id,
        String accountNumber,
        String accountName,
        BigDecimal balance,
        String currencyCode
) {
}

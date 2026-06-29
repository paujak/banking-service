package com.banking.service.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * API response confirming a successful deposit.
 * {@code null} fields are excluded from the JSON output.
 *
 * @param transactionId UUID of the created deposit transaction
 * @param type          transaction type (always {@code "DEPOSIT"})
 * @param currencyCode  ISO 4217 currency code
 * @param amount        deposited amount
 * @param balanceAfter  account balance after the deposit
 * @param description   optional free-text note
 * @param timestamp     time the transaction was recorded
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DepositResponse(
        UUID transactionId,
        String type,
        String currencyCode,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String description,
        Instant timestamp
) {
}

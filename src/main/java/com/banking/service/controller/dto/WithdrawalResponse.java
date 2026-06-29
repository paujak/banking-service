package com.banking.service.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * API response confirming a successful withdrawal.
 * {@code null} fields are excluded from the JSON output.
 *
 * @param transactionId UUID of the created withdrawal transaction
 * @param type          transaction type (always {@code "WITHDRAWAL"})
 * @param currencyCode  ISO 4217 currency code
 * @param amount        withdrawn amount
 * @param balanceAfter  account balance after the withdrawal
 * @param description   optional free-text note
 * @param timestamp     time the transaction was recorded
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WithdrawalResponse(
        UUID transactionId,
        String type,
        String currencyCode,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String description,
        Instant timestamp
) {
}

package com.banking.service.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * API response representing a single entry in the transaction history.
 * {@code null} fields (e.g. {@code appliedRate} for non-exchange transactions) are excluded from the JSON output.
 *
 * @param transactionId UUID of the transaction
 * @param type          transaction type string
 * @param currencyCode  ISO 4217 currency code
 * @param appliedRate   exchange rate applied; {@code null} for non-exchange transactions
 * @param amount        transaction amount
 * @param balanceAfter  account balance after this transaction
 * @param description   optional free-text note
 * @param timestamp     time the transaction was recorded
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransactionResponse(
        UUID transactionId,
        String type,
        String currencyCode,
        BigDecimal appliedRate,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String description,
        Instant timestamp
) {
}

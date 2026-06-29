package com.banking.service.service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Service-layer read model for a completed transaction.
 *
 * @param id           transaction UUID
 * @param type         transaction type string
 * @param currency     currency of the transaction
 * @param appliedRate  exchange rate applied; {@code null} for non-exchange transactions
 * @param amount       transaction amount
 * @param balanceAfter account balance immediately after this transaction
 * @param description  optional free-text note
 * @param timestamp    time the transaction was recorded
 */
public record TransactionResultDTO(
        UUID id,
        String type,
        CurrencyDTO currency,
        BigDecimal appliedRate,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String description,
        Instant timestamp
) {
}

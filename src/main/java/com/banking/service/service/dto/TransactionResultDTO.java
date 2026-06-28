package com.banking.service.service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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

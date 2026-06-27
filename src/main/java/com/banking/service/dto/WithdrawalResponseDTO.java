package com.banking.service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record WithdrawalResponseDTO(
        UUID transactionId,
        String type,
        String currencyCode,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String description,
        Instant timestamp
) {
}

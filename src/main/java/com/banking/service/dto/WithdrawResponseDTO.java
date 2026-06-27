package com.banking.service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record WithdrawResponseDTO(
        UUID transactionId,
        String type,
        String currencyCode,
        BigDecimal amountWithdrawn,
        BigDecimal balanceAfter,
        String description,
        Instant timestamp
) {
}

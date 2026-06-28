package com.banking.service.service.dto;

import com.banking.service.constant.TransactionType;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;

@Builder
public record TransactionRequestDTO(
        BigDecimal amount,
        String description,
        TransactionType type,
        UUID sourceAccountId,
        UUID destinationAccountId
) {
}

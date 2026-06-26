package com.banking.service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponseDTO(
        UUID id,
        String accountNumber,
        String accountName,
        BigDecimal balance,
        String currencyCode
) {
}

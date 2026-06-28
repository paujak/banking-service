package com.banking.service.service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountDTO(
        UUID id,
        String accountNumber,
        String accountName,
        BigDecimal balance,
        String currencyCode
) {
}

package com.banking.service.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccountResponse(
        UUID id,
        String accountNumber,
        String accountName,
        BigDecimal balance,
        String currencyCode
) {
}

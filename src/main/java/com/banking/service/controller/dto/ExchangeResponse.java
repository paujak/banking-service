package com.banking.service.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExchangeResponse(
        BigDecimal amountInTargetCurrency,
        BigDecimal amountInSourceCurrency,
        String sourceCurrencyCode,
        String targetCurrencyCode,
        BigDecimal appliedRate,
        String description,
        Instant timestamp
) {
}

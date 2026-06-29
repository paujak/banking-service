package com.banking.service.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;

/**
 * API response confirming a successful currency exchange.
 * {@code null} fields are excluded from the JSON output.
 *
 * @param amountInTargetCurrency amount credited to the destination account in the target currency
 * @param amountInSourceCurrency amount debited from the source account
 * @param sourceCurrencyCode     ISO 4217 code of the source currency
 * @param targetCurrencyCode     ISO 4217 code of the target currency
 * @param appliedRate            exchange rate that was applied
 * @param description            optional free-text note
 * @param timestamp              time the exchange was recorded
 */
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

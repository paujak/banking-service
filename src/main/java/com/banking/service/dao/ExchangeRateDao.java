package com.banking.service.dao;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Data access abstraction for exchange-rate lookups.
 */
public interface ExchangeRateDao {

    /**
     * Looks up the exchange rate from one currency to another.
     *
     * @param fromCurrencyCode ISO 4217 code of the source currency (e.g. {@code "EUR"})
     * @param toCurrencyCode   ISO 4217 code of the target currency (e.g. {@code "USD"})
     * @return an {@link Optional} containing the rate, or empty if no rate is defined
     */
    Optional<BigDecimal> getExchangeRate(String fromCurrencyCode, String toCurrencyCode);
}

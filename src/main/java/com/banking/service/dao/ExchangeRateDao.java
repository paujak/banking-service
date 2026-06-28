package com.banking.service.dao;

import java.math.BigDecimal;
import java.util.Optional;

public interface ExchangeRateDao {
    Optional<BigDecimal> getExchangeRate(String fromCurrencyCode, String toCurrencyCode);
}

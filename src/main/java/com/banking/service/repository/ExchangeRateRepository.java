package com.banking.service.repository;

import com.banking.service.entity.ExchangeRate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, UUID> {
    Optional<ExchangeRate> getExchangeRatesByFromCurrencyCodeAndToCurrencyCode(String fromCurrencyCode, String toCurrencyCode);
}

package com.banking.service.repository;

import com.banking.service.entity.ExchangeRate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link ExchangeRate} entities.
 */
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, UUID> {

    /**
     * Looks up an exchange rate by source and target currency codes.
     *
     * @param fromCurrencyCode ISO 4217 code of the source currency
     * @param toCurrencyCode   ISO 4217 code of the target currency
     * @return an {@link Optional} containing the exchange rate entity, or empty if not defined
     */
    Optional<ExchangeRate> getExchangeRatesByFromCurrencyCodeAndToCurrencyCode(String fromCurrencyCode, String toCurrencyCode);
}

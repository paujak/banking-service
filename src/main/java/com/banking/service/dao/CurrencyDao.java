package com.banking.service.dao;

import com.banking.service.entity.Currency;
import java.util.Optional;

/**
 * Data access abstraction for {@link Currency} entities.
 */
public interface CurrencyDao {

    /**
     * Finds a currency by its ISO 4217 code.
     *
     * @param code the three-letter ISO 4217 code (e.g. {@code "EUR"})
     * @return an {@link Optional} containing the currency, or empty if not found
     */
    Optional<Currency> findByCode(String code);

    /**
     * Persists a new or updated currency.
     *
     * @param currency the currency to save
     * @return the saved entity
     */
    Currency save(Currency currency);
}

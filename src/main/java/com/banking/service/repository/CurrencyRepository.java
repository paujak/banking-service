package com.banking.service.repository;

import com.banking.service.entity.Currency;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link Currency} entities.
 */
@Repository
public interface CurrencyRepository extends JpaRepository<Currency, UUID> {

    /**
     * Finds a currency by its ISO 4217 code.
     *
     * @param code the three-letter ISO 4217 currency code
     * @return an {@link Optional} containing the currency, or empty if not found
     */
    Optional<Currency> findByCode(String code);
}

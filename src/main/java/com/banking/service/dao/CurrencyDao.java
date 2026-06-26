package com.banking.service.dao;

import com.banking.service.entity.Currency;
import java.util.Optional;

public interface CurrencyDao {
    Optional<Currency> findByCode(String code);

    Currency save(Currency currency);
}

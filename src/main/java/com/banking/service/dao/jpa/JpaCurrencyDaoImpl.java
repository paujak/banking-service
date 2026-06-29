package com.banking.service.dao.jpa;

import com.banking.service.dao.CurrencyDao;
import com.banking.service.entity.Currency;
import com.banking.service.repository.CurrencyRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * JPA-backed implementation of {@link CurrencyDao}.
 */
@Component
public class JpaCurrencyDaoImpl implements CurrencyDao {
    
    private final CurrencyRepository currencyRepository;
    
    public JpaCurrencyDaoImpl(CurrencyRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }
    
    /** {@inheritDoc} */
    @Override
    public Optional<Currency> findByCode(String code) {
        return currencyRepository.findByCode(code);
    }

    /** {@inheritDoc} */
    @Override
    public Currency save(Currency currency) {
        return currencyRepository.save(currency);
    }
}

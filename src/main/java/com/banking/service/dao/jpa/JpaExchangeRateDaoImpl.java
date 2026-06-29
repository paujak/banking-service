package com.banking.service.dao.jpa;

import com.banking.service.dao.ExchangeRateDao;
import com.banking.service.entity.ExchangeRate;
import com.banking.service.repository.ExchangeRateRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * JPA-backed implementation of {@link ExchangeRateDao}.
 */
@Component
public class JpaExchangeRateDaoImpl implements ExchangeRateDao {
    
    private final ExchangeRateRepository exchangeRateRepository;
    
    public  JpaExchangeRateDaoImpl(ExchangeRateRepository exchangeRateRepository) {
        this.exchangeRateRepository = exchangeRateRepository;
    }
    
    /** {@inheritDoc} */
    @Override
    public Optional<BigDecimal> getExchangeRate(String sourceCurrencyCode, String destinationCurrencyCode) {
        return exchangeRateRepository.getExchangeRatesByFromCurrencyCodeAndToCurrencyCode(sourceCurrencyCode, destinationCurrencyCode).map(ExchangeRate::getRate);
    }
}

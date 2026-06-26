package com.banking.service.config;

import com.banking.service.dao.AccountDao;
import com.banking.service.dao.CurrencyDao;
import com.banking.service.entity.Account;
import com.banking.service.entity.Currency;
import com.banking.service.entity.User;
import com.banking.service.repository.UserRepository;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DbInitializer implements CommandLineRunner {
    
    private AccountDao accountDao;
    private CurrencyDao currencyDao;
    private UserRepository userRepository;
    
    public DbInitializer(AccountDao accountDao, CurrencyDao currencyDao, UserRepository userRepository) {
        this.accountDao = accountDao;
        this.currencyDao = currencyDao;
        this.userRepository = userRepository;
    }
    
    @Override
    public void run(String... args) throws Exception {
        Currency eur = currencyDao.findByCode("EUR")
                .orElseGet(() -> currencyDao.save(new Currency("EUR", "Euro", true)));

        Currency usd = currencyDao.findByCode("USD")
                .orElseGet(() -> currencyDao.save(new Currency("USD", "US Dollar", true)));

        User demoUser = userRepository.findByUsername("john_doe")
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .username("john_doe")
                            .fullName("John Doe")
                            .email("john.doe@example.com")
                            .build();
                    return userRepository.save(newUser);
                });

        if (accountDao.getByUserId(demoUser.getId()).isEmpty()) {

            Account primaryEur = Account.builder()
                    .user(demoUser)
                    .accountNumber("LT993000123456789001")
                    .accountName("Primary Checking (EUR)")
                    .currency(eur)
                    .initialBalance(new BigDecimal("5250.75"))
                    .build();

            Account travelUsd = Account.builder()
                    .user(demoUser)
                    .accountNumber("LT993000123456789002")
                    .accountName("US Travel Vault")
                    .currency(usd)
                    .initialBalance(new BigDecimal("1200.00"))
                    .build();

            Account savingsEur = Account.builder()
                    .user(demoUser)
                    .accountNumber("LT993000123456789003")
                    .accountName("")
                    .currency(eur)
                    .initialBalance(new BigDecimal("15000.00"))
                    .build();

            accountDao.save(primaryEur);
            accountDao.save(travelUsd);
            accountDao.save(savingsEur);

            System.out.println(">> Database successfully seeded with 1 User and 3 Accounts.");
        } else {
            System.out.println(">> Database already contains accounts. Skipping seeding initialization.");
        }
    }
}

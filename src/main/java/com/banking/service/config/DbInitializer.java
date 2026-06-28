package com.banking.service.config;

import com.banking.service.constant.TransactionType;
import com.banking.service.dao.CurrencyDao;
import com.banking.service.entity.Account;
import com.banking.service.entity.Currency;
import com.banking.service.entity.ExchangeRate;
import com.banking.service.entity.Transaction;
import com.banking.service.entity.User;
import com.banking.service.repository.AccountRepository;
import com.banking.service.repository.ExchangeRateRepository;
import com.banking.service.repository.TransactionRepository;
import com.banking.service.repository.UserRepository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DbInitializer implements CommandLineRunner {

    static final UUID DEMO_USER_ID = UUID.fromString("cfb095a4-bed2-4aa4-93c6-abf73d2dbbe1");

    private final CurrencyDao currencyDao;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final TransactionRepository transactionRepository;
    private final JdbcTemplate jdbcTemplate;

    public DbInitializer(CurrencyDao currencyDao,
                         UserRepository userRepository,
                         AccountRepository accountRepository,
                         ExchangeRateRepository exchangeRateRepository,
                         TransactionRepository transactionRepository,
                         JdbcTemplate jdbcTemplate) {
        this.currencyDao = currencyDao;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.exchangeRateRepository = exchangeRateRepository;
        this.transactionRepository = transactionRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        Currency eur = ensureCurrency("EUR", "Euro");
        Currency usd = ensureCurrency("USD", "US Dollar");
        Currency sek = ensureCurrency("SEK", "Swedish Krona");
        Currency gbp = ensureCurrency("GBP", "British Pound");
        Currency vnd = ensureCurrency("VND", "Vietnamese Dong");

        seedExchangeRates(eur, usd, sek, gbp, vnd);

        // INSERT IGNORE with UUID_TO_BIN ensures binary(16) UUID matches Hibernate's storage format
        jdbcTemplate.update(
                "INSERT IGNORE INTO `user` (id, username, full_name, email, created_at) VALUES (UUID_TO_BIN(?), ?, ?, ?, ?)",
                DEMO_USER_ID.toString(), "john_doe", "John Doe", "john.doe@example.com",
                Timestamp.from(Instant.now()));
        User demoUser = userRepository.findByUsername("john_doe").orElseThrow();

        if (accountRepository.getByUserId(demoUser.getId()).isEmpty()) {
            Account primaryEur = accountRepository.save(Account.builder()
                    .user(demoUser)
                    .accountNumber("LT993000123456789001")
                    .accountName("Primary Checking (EUR)")
                    .currency(eur)
                    .initialBalance(new BigDecimal("5250.75"))
                    .build());

            Account travelUsd = accountRepository.save(Account.builder()
                    .user(demoUser)
                    .accountNumber("LT993000123456789002")
                    .accountName("US Travel Vault")
                    .currency(usd)
                    .initialBalance(new BigDecimal("1200.00"))
                    .build());

            accountRepository.save(Account.builder()
                    .user(demoUser)
                    .accountNumber("LT993000123456789003")
                    .accountName("EUR Savings")
                    .currency(eur)
                    .initialBalance(new BigDecimal("15000.00"))
                    .build());

            seedTransactions(primaryEur, eur);
            seedTransactions(travelUsd, usd);

            System.out.println(">> Seeded 1 User (id=" + demoUser.getId() + "), 3 Accounts, and sample transactions.");
        } else {
            System.out.println(">> DB already seeded. Demo user id=" + demoUser.getId());
        }
    }

    private Currency ensureCurrency(String code, String name) {
        return currencyDao.findByCode(code)
                .orElseGet(() -> currencyDao.save(new Currency(code, name)));
    }

    private void seedExchangeRates(Currency eur, Currency usd, Currency sek, Currency gbp, Currency vnd) {
        Object[][] rates = {
            {eur, usd, "1.08"},      {eur, sek, "11.20"},    {eur, gbp, "0.85"},      {eur, vnd, "27500.00"},
            {usd, eur, "0.926"},     {usd, sek, "10.37"},    {usd, gbp, "0.787"},     {usd, vnd, "25463.00"},
            {sek, eur, "0.0893"},    {sek, usd, "0.0964"},   {sek, gbp, "0.0759"},    {sek, vnd, "2455.00"},
            {gbp, eur, "1.176"},     {gbp, usd, "1.270"},    {gbp, sek, "13.18"},     {gbp, vnd, "32353.00"},
            {vnd, eur, "0.0000364"}, {vnd, usd, "0.0000393"},{vnd, sek, "0.000407"},  {vnd, gbp, "0.0000309"}
        };
        for (Object[] row : rates) {
            Currency from = (Currency) row[0];
            Currency to   = (Currency) row[1];
            if (exchangeRateRepository.getExchangeRatesByFromCurrencyCodeAndToCurrencyCode(from.getCode(), to.getCode()).isEmpty()) {
                exchangeRateRepository.save(ExchangeRate.builder()
                        .fromCurrency(from)
                        .toCurrency(to)
                        .rate(new BigDecimal((String) row[2]))
                        .build());
            }
        }
    }

    private void seedTransactions(Account account, Currency currency) {
        Instant now = Instant.now();
        if (currency.getCode().equals("EUR")) {
            // 25 transactions spanning ~1 year; final balanceAfter matches account.balance (5250.75)
            Object[][] txData = {
                {365, TransactionType.DEPOSIT,    "1500.00", "1500.00",  "Initial deposit"},
                {350, TransactionType.DEPOSIT,    "1000.00", "2500.00",  "Salary"},
                {330, TransactionType.WITHDRAWAL,  "300.00", "2200.00",  "Rent"},
                {310, TransactionType.DEPOSIT,    "1800.00", "4000.00",  "Freelance payment"},
                {290, TransactionType.WITHDRAWAL,  "500.00", "3500.00",  "Groceries"},
                {270, TransactionType.DEPOSIT,    "2000.00", "5500.00",  "Bonus"},
                {250, TransactionType.WITHDRAWAL,  "750.00", "4750.00",  "Utilities"},
                {230, TransactionType.DEPOSIT,    "1500.00", "6250.00",  "Salary"},
                {210, TransactionType.WITHDRAWAL,  "400.00", "5850.00",  "Insurance"},
                {190, TransactionType.DEPOSIT,    "1200.00", "7050.00",  "Consulting fee"},
                {170, TransactionType.WITHDRAWAL,  "800.00", "6250.00",  "Car repair"},
                {150, TransactionType.DEPOSIT,    "2000.00", "8250.00",  "Salary"},
                {130, TransactionType.WITHDRAWAL, "1500.00", "6750.00",  "Holiday trip"},
                {110, TransactionType.DEPOSIT,     "600.00", "7350.00",  "Refund"},
                { 90, TransactionType.WITHDRAWAL,  "350.00", "7000.00",  "Subscription"},
                { 75, TransactionType.DEPOSIT,    "1000.00", "8000.00",  "Salary"},
                { 60, TransactionType.WITHDRAWAL,  "500.00", "7500.00",  "Phone bill"},
                { 45, TransactionType.WITHDRAWAL, "1200.00", "6300.00",  "New laptop"},
                { 35, TransactionType.DEPOSIT,     "700.00", "7000.00",  "Side project"},
                { 28, TransactionType.WITHDRAWAL,  "300.00", "6700.00",  "Dining out"},
                { 21, TransactionType.WITHDRAWAL,  "500.00", "6200.00",  "Gym membership"},
                { 14, TransactionType.DEPOSIT,    "1000.00", "7200.00",  "Salary"},
                { 10, TransactionType.WITHDRAWAL, "1500.00", "5700.00",  "Rent"},
                {  7, TransactionType.WITHDRAWAL, "1000.00", "4700.00",  "Tax payment"},
                {  3, TransactionType.DEPOSIT,     "550.75", "5250.75",  "Cashback reward"},
            };
            saveTransactions(account, currency, txData, now);
        } else if (currency.getCode().equals("USD")) {
            // 8 transactions; final balanceAfter matches account.balance (1200.00)
            Object[][] txData = {
                {180, TransactionType.DEPOSIT,    "500.00",  "500.00",  "Initial USD deposit"},
                {150, TransactionType.DEPOSIT,    "800.00", "1300.00",  "Wire transfer"},
                {120, TransactionType.WITHDRAWAL, "200.00", "1100.00",  "Online purchase"},
                { 90, TransactionType.DEPOSIT,    "600.00", "1700.00",  "Freelance USD"},
                { 60, TransactionType.WITHDRAWAL, "300.00", "1400.00",  "Subscription"},
                { 30, TransactionType.WITHDRAWAL, "400.00", "1000.00",  "Travel expense"},
                { 14, TransactionType.DEPOSIT,    "350.00", "1350.00",  "Refund"},
                {  5, TransactionType.WITHDRAWAL, "150.00", "1200.00",  "Food delivery"},
            };
            saveTransactions(account, currency, txData, now);
        }
    }

    private void saveTransactions(Account account, Currency currency, Object[][] txData, Instant now) {
        for (Object[] row : txData) {
            int daysAgo = (int) row[0];
            transactionRepository.save(Transaction.builder()
                    .account(account)
                    .currency(currency)
                    .type((TransactionType) row[1])
                    .amount(new BigDecimal((String) row[2]))
                    .balanceAfter(new BigDecimal((String) row[3]))
                    .description((String) row[4])
                    .timestamp(now.minus(daysAgo, ChronoUnit.DAYS))
                    .build());
        }
    }
}

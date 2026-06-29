package com.banking.service.integration;

import com.banking.service.entity.Account;
import com.banking.service.entity.Currency;
import com.banking.service.entity.ExchangeRate;
import com.banking.service.entity.User;
import com.banking.service.repository.AccountRepository;
import com.banking.service.repository.CurrencyRepository;
import com.banking.service.repository.ExchangeRateRepository;
import com.banking.service.repository.TransactionRepository;
import com.banking.service.repository.UserRepository;
import com.banking.service.service.ExternalLoggingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class AccountIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @LocalServerPort int port;

    @Autowired AccountRepository accountRepository;
    @Autowired UserRepository userRepository;
    @Autowired CurrencyRepository currencyRepository;
    @Autowired TransactionRepository transactionRepository;
    @Autowired ExchangeRateRepository exchangeRateRepository;

    @MockitoBean ExternalLoggingService externalLoggingService;

    RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();
        exchangeRateRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        currencyRepository.deleteAllInBatch();

        this.restTemplate = new RestTemplate();
        this.restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false; // never throw — let each test assert the status code manually
            }
        });
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private Currency createAndSaveCurrency(String code, String name) {
        return currencyRepository.save(Currency.builder().code(code).name(name).build());
    }

    private User createAndSaveUser() {
        return userRepository.save(User.builder()
                .username("user-" + UUID.randomUUID())
                .fullName("Test User")
                .email("test-" + UUID.randomUUID() + "@example.com")
                .build());
    }

    private Account createAndSaveAccount(User user, Currency currency, BigDecimal balance) {
        return accountRepository.save(Account.builder()
                .accountNumber("ACC-" + UUID.randomUUID())
                .accountName("Test Account")
                .user(user)
                .currency(currency)
                .initialBalance(balance)
                .build());
    }

    private HttpEntity<String> jsonBody(String body) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    // ── GET /api/users/{userId}/accounts ─────────────────────────────────────

    @Test
    void shouldReturnAccountsForUser_WhenUserHasAccounts() {
        var currency = createAndSaveCurrency("EUR", "Euro");
        var user     = createAndSaveUser();
        createAndSaveAccount(user, currency, new BigDecimal("500.00"));

        var response = restTemplate.getForEntity(
                url("/api/users/{userId}/accounts"), String.class, user.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("ACC-");
    }

    @Test
    void shouldReturnEmptyList_WhenUserHasNoAccounts() {
        var user = createAndSaveUser();

        var response = restTemplate.getForEntity(
                url("/api/users/{userId}/accounts"), String.class, user.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("[]");
    }

    @Test
    void shouldReturn404_WhenUserNotFound() {
        var response = restTemplate.getForEntity(
                url("/api/users/{userId}/accounts"), String.class, UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── GET /api/accounts/{accountId}/balance ────────────────────────────────

    @Test
    void shouldReturnAccountBalance_WhenAccountExists() {
        var currency = createAndSaveCurrency("EUR", "Euro");
        var user     = createAndSaveUser();
        var account  = createAndSaveAccount(user, currency, new BigDecimal("1234.56"));

        var response = restTemplate.getForEntity(
                url("/api/accounts/{accountId}"), String.class, account.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("1234.56");
    }

    @Test
    void shouldReturn422_WhenAccountDoesNotExist() {
        var response = restTemplate.getForEntity(
                url("/api/accounts/{accountId}"), String.class, UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    // ── GET /api/accounts/{accountId}/transactions ───────────────────────────

    @Test
    void shouldReturnTransactionHistory_WhenAccountExists() {
        var currency = createAndSaveCurrency("EUR", "Euro");
        var user     = createAndSaveUser();
        var account  = createAndSaveAccount(user, currency, new BigDecimal("300.00"));

        var response = restTemplate.getForEntity(
                url("/api/accounts/{accountId}/transactions"), String.class, account.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("[]");
    }

    @Test
    void shouldReturn422_WhenGettingTransactionsForNonExistentAccount() {
        var response = restTemplate.getForEntity(
                url("/api/accounts/{accountId}/transactions"), String.class, UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    // ── POST /api/accounts/{accountId}/transactions/deposit ──────────────────

    @Test
    void shouldDepositMoneyAndPersistTransaction_WhenAccountExists() {
        var currency = createAndSaveCurrency("EUR", "Euro");
        var user     = createAndSaveUser();
        var account  = createAndSaveAccount(user, currency, new BigDecimal("500.00"));

        var response = restTemplate.exchange(
                url("/api/accounts/{id}/transactions/deposit"),
                HttpMethod.POST,
                jsonBody("{\"amount\": 100.00, \"description\": \"Top-up\"}"),
                String.class, account.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(transactionRepository.findByAccountIdOrderByTimestampDesc(account.getId())).hasSize(1);
    }

    @Test
    void shouldReturn422_WhenDepositingToNonExistentAccount() {
        var response = restTemplate.exchange(
                url("/api/accounts/{id}/transactions/deposit"),
                HttpMethod.POST,
                jsonBody("{\"amount\": 100.00}"),
                String.class, UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(transactionRepository.count()).isZero();
    }

    // ── POST /api/accounts/{accountId}/transactions/withdraw ─────────────────

    @Test
    void shouldWithdrawMoneyAndPersistTransaction_WhenSufficientFunds() {
        var currency = createAndSaveCurrency("EUR", "Euro");
        var user     = createAndSaveUser();
        var account  = createAndSaveAccount(user, currency, new BigDecimal("500.00"));

        var response = restTemplate.exchange(
                url("/api/accounts/{id}/transactions/withdraw"),
                HttpMethod.POST,
                jsonBody("{\"amount\": 100.00, \"description\": \"ATM\"}"),
                String.class, account.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(transactionRepository.findByAccountIdOrderByTimestampDesc(account.getId())).hasSize(1);
    }

    @Test
    void shouldReturn400AndNotPersistTransaction_WhenInsufficientFunds() {
        var currency = createAndSaveCurrency("EUR", "Euro");
        var user     = createAndSaveUser();
        var account  = createAndSaveAccount(user, currency, new BigDecimal("50.00"));

        var response = restTemplate.exchange(
                url("/api/accounts/{id}/transactions/withdraw"),
                HttpMethod.POST,
                jsonBody("{\"amount\": 500.00}"),
                String.class, account.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(transactionRepository.count()).isZero();
    }

    // ── POST /api/accounts/{accountId}/transactions/currency-exchange ─────────

    @Test
    void shouldExchangeCurrencyAndPersistTransactions_WhenValidAccounts() {
        var eur     = createAndSaveCurrency("EUR", "Euro");
        var usd     = createAndSaveCurrency("USD", "US Dollar");
        var user    = createAndSaveUser();
        var eurAcct = createAndSaveAccount(user, eur, new BigDecimal("500.00"));
        var usdAcct = createAndSaveAccount(user, usd, new BigDecimal("200.00"));
        exchangeRateRepository.save(ExchangeRate.builder()
                .fromCurrency(eur).toCurrency(usd).rate(new BigDecimal("1.10")).build());

        var body = String.format(
                "{\"destinationAccountId\": \"%s\", \"amountToTransfer\": 100.00, \"description\": \"FX\"}",
                usdAcct.getId());

        var response = restTemplate.exchange(
                url("/api/accounts/{id}/transactions/currency-exchange"),
                HttpMethod.POST, jsonBody(body), String.class, eurAcct.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(transactionRepository.count()).isEqualTo(2);
    }

    @Test
    void shouldReturn400AndNotPersistTransactions_WhenExchangingWithinSameAccount() {
        var currency = createAndSaveCurrency("EUR", "Euro");
        var user     = createAndSaveUser();
        var account  = createAndSaveAccount(user, currency, new BigDecimal("500.00"));
        var body     = String.format(
                "{\"destinationAccountId\": \"%s\", \"amountToTransfer\": 100.00}", account.getId());

        var response = restTemplate.exchange(
                url("/api/accounts/{id}/transactions/currency-exchange"),
                HttpMethod.POST, jsonBody(body), String.class, account.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(transactionRepository.count()).isZero();
    }
}

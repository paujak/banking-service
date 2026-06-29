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
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
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

    CloseableHttpClient httpClient;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();
        exchangeRateRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        currencyRepository.deleteAllInBatch();

        httpClient = HttpClients.createDefault();
    }

    @AfterEach
    void tearDown() throws IOException {
        httpClient.close();
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

    private record HttpResponse(int status, String body) {}

    private HttpResponse get(String url) throws IOException {
        var request = new BasicClassicHttpRequest(Method.GET, url);
        return httpClient.execute(request, response ->
                new HttpResponse(response.getCode(), EntityUtils.toString(response.getEntity())));
    }

    private HttpResponse post(String url, String body) throws IOException {
        var request = new BasicClassicHttpRequest(Method.POST, url);
        request.setEntity(HttpEntities.create(body, ContentType.APPLICATION_JSON));
        return httpClient.execute(request, response ->
                new HttpResponse(response.getCode(), EntityUtils.toString(response.getEntity())));
    }

    // ── GET /api/users/{userId}/accounts ─────────────────────────────────────

    @Test
    void shouldReturnAccountsForUser_WhenUserHasAccounts() throws IOException {
        var currency = createAndSaveCurrency("EUR", "Euro");
        var user     = createAndSaveUser();
        createAndSaveAccount(user, currency, new BigDecimal("500.00"));

        var response = get(url("/api/users/" + user.getId() + "/accounts"));

        assertThat(response.status()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body()).contains("ACC-");
    }

    @Test
    void shouldReturnEmptyList_WhenUserHasNoAccounts() throws IOException {
        var user = createAndSaveUser();

        var response = get(url("/api/users/" + user.getId() + "/accounts"));

        assertThat(response.status()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body()).isEqualTo("[]");
    }

    @Test
    void shouldReturn404_WhenUserNotFound() throws IOException {
        var response = get(url("/api/users/" + UUID.randomUUID() + "/accounts"));

        assertThat(response.status()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    // ── GET /api/accounts/{accountId} ────────────────────────────────────────

    @Test
    void shouldReturnAccountBalance_WhenAccountExists() throws IOException {
        var currency = createAndSaveCurrency("EUR", "Euro");
        var user     = createAndSaveUser();
        var account  = createAndSaveAccount(user, currency, new BigDecimal("1234.56"));

        var response = get(url("/api/accounts/" + account.getId()));

        assertThat(response.status()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body()).contains("1234.56");
    }

    @Test
    void shouldReturn422_WhenAccountDoesNotExist() throws IOException {
        var response = get(url("/api/accounts/" + UUID.randomUUID()));

        assertThat(response.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT.value());
    }

    // ── GET /api/accounts/{accountId}/transactions ───────────────────────────

    @Test
    void shouldReturnTransactionHistory_WhenAccountExists() throws IOException {
        var currency = createAndSaveCurrency("EUR", "Euro");
        var user     = createAndSaveUser();
        var account  = createAndSaveAccount(user, currency, new BigDecimal("300.00"));

        var response = get(url("/api/accounts/" + account.getId() + "/transactions"));

        assertThat(response.status()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body()).isEqualTo("[]");
    }

    @Test
    void shouldReturn422_WhenGettingTransactionsForNonExistentAccount() throws IOException {
        var response = get(url("/api/accounts/" + UUID.randomUUID() + "/transactions"));

        assertThat(response.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT.value());
    }

    // ── POST /api/accounts/{accountId}/transactions/deposit ──────────────────

    @Test
    void shouldDepositMoneyAndPersistTransaction_WhenAccountExists() throws IOException {
        var currency = createAndSaveCurrency("EUR", "Euro");
        var user     = createAndSaveUser();
        var account  = createAndSaveAccount(user, currency, new BigDecimal("500.00"));

        var response = post(
                url("/api/accounts/" + account.getId() + "/transactions/deposit"),
                "{\"amount\": 100.00, \"description\": \"Top-up\"}");

        assertThat(response.status()).isEqualTo(HttpStatus.OK.value());
        assertThat(transactionRepository.findByAccountIdOrderByTimestampDesc(account.getId())).hasSize(1);
    }

    @Test
    void shouldReturn422_WhenDepositingToNonExistentAccount() throws IOException {
        var response = post(
                url("/api/accounts/" + UUID.randomUUID() + "/transactions/deposit"),
                "{\"amount\": 100.00}");

        assertThat(response.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT.value());
        assertThat(transactionRepository.count()).isZero();
    }

    // ── POST /api/accounts/{accountId}/transactions/withdraw ─────────────────

    @Test
    void shouldWithdrawMoneyAndPersistTransaction_WhenSufficientFunds() throws IOException {
        var currency = createAndSaveCurrency("EUR", "Euro");
        var user     = createAndSaveUser();
        var account  = createAndSaveAccount(user, currency, new BigDecimal("500.00"));

        var response = post(
                url("/api/accounts/" + account.getId() + "/transactions/withdraw"),
                "{\"amount\": 100.00, \"description\": \"ATM\"}");

        assertThat(response.status()).isEqualTo(HttpStatus.OK.value());
        assertThat(transactionRepository.findByAccountIdOrderByTimestampDesc(account.getId())).hasSize(1);
    }

    @Test
    void shouldReturn400AndNotPersistTransaction_WhenInsufficientFunds() throws IOException {
        var currency = createAndSaveCurrency("EUR", "Euro");
        var user     = createAndSaveUser();
        var account  = createAndSaveAccount(user, currency, new BigDecimal("50.00"));

        var response = post(
                url("/api/accounts/" + account.getId() + "/transactions/withdraw"),
                "{\"amount\": 500.00}");

        assertThat(response.status()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(transactionRepository.count()).isZero();
    }

    // ── POST /api/accounts/{accountId}/transactions/currency-exchange ─────────

    @Test
    void shouldExchangeCurrencyAndPersistTransactions_WhenValidAccounts() throws IOException {
        var eur     = createAndSaveCurrency("EUR", "Euro");
        var usd     = createAndSaveCurrency("USD", "US Dollar");
        var user    = createAndSaveUser();
        var eurAcct = createAndSaveAccount(user, eur, new BigDecimal("500.00"));
        var usdAcct = createAndSaveAccount(user, usd, new BigDecimal("200.00"));
        exchangeRateRepository.save(ExchangeRate.builder()
                .fromCurrency(eur).toCurrency(usd).rate(new BigDecimal("1.10")).build());

        var response = post(
                url("/api/accounts/" + eurAcct.getId() + "/transactions/currency-exchange"),
                "{\"destinationAccountId\": \"" + usdAcct.getId() + "\", \"amountToTransfer\": 100.00, \"description\": \"FX\"}");

        assertThat(response.status()).isEqualTo(HttpStatus.OK.value());
        assertThat(transactionRepository.count()).isEqualTo(2);
    }

    @Test
    void shouldReturn400AndNotPersistTransactions_WhenExchangingWithinSameAccount() throws IOException {
        var currency = createAndSaveCurrency("EUR", "Euro");
        var user     = createAndSaveUser();
        var account  = createAndSaveAccount(user, currency, new BigDecimal("500.00"));

        var response = post(
                url("/api/accounts/" + account.getId() + "/transactions/currency-exchange"),
                "{\"destinationAccountId\": \"" + account.getId() + "\", \"amountToTransfer\": 100.00}");

        assertThat(response.status()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(transactionRepository.count()).isZero();
    }
}

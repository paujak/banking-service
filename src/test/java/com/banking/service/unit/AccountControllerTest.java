package com.banking.service.unit;

import com.banking.service.controller.AccountController;
import com.banking.service.controller.dto.AccountResponse;
import com.banking.service.controller.dto.DepositResponse;
import com.banking.service.controller.dto.WithdrawalResponse;
import com.banking.service.exception.AccountNotFoundException;
import com.banking.service.exception.GlobalExceptionHandler;
import com.banking.service.mapper.AccountMapper;
import com.banking.service.mapper.TransactionMapper;
import com.banking.service.service.AccountService;
import com.banking.service.service.ExternalLoggingService;
import com.banking.service.service.dto.AccountDTO;
import com.banking.service.service.dto.CurrencyDTO;
import com.banking.service.controller.dto.ExchangeResponse;
import com.banking.service.service.dto.ExchangeResultDTO;
import com.banking.service.service.dto.TransactionResultDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock AccountService accountService;
    @Mock AccountMapper accountMapper;
    @Mock TransactionMapper transactionMapper;
    @Mock ExternalLoggingService externalLoggingService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new AccountController(
                accountService, accountMapper, transactionMapper, externalLoggingService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static final UUID ACCOUNT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID    = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void shouldReturnAccounts_WhenUserHasAccounts() throws Exception {
        var dto      = new AccountDTO(ACCOUNT_ID, "ACC001", "Main", new BigDecimal("100.00"), "EUR");
        var response = new AccountResponse(ACCOUNT_ID, "ACC001", "Main", new BigDecimal("100.00"), "EUR");
        when(accountService.getAccountsByUserId(USER_ID)).thenReturn(List.of(dto));
        when(accountMapper.toResponseList(List.of(dto))).thenReturn(List.of(response));

        mockMvc.perform(get("/api/users/{userId}/accounts", USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void shouldReturnEmptyList_WhenUserHasNoAccounts() throws Exception {
        when(accountService.getAccountsByUserId(USER_ID)).thenReturn(List.of());
        when(accountMapper.toResponseList(List.of())).thenReturn(List.of());

        mockMvc.perform(get("/api/users/{userId}/accounts", USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[]"));
    }

    @Test
    void shouldReturnAccountBalance_WhenAccountExists() throws Exception {
        var dto      = new AccountDTO(ACCOUNT_ID, "ACC001", "Main", new BigDecimal("500.00"), "EUR");
        var response = new AccountResponse(ACCOUNT_ID, "ACC001", "Main", new BigDecimal("500.00"), "EUR");
        when(accountService.getAccount(ACCOUNT_ID)).thenReturn(dto);
        when(accountMapper.toResponse(dto)).thenReturn(response);

        mockMvc.perform(get("/api/accounts/{accountId}/balance", ACCOUNT_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void shouldReturn422_WhenAccountNotFound() throws Exception {
        when(accountService.getAccount(ACCOUNT_ID))
                .thenThrow(new AccountNotFoundException("Account not found"));

        mockMvc.perform(get("/api/accounts/{accountId}/balance", ACCOUNT_ID))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void shouldDepositMoney_WhenRequestIsValid() throws Exception {
        var txResult    = new TransactionResultDTO(UUID.randomUUID(), "DEPOSIT",
                new CurrencyDTO("EUR", "Euro"), null,
                new BigDecimal("100.00"), new BigDecimal("600.00"), "Test deposit", Instant.now());
        var depositResp = new DepositResponse(UUID.randomUUID(), "DEPOSIT", "EUR",
                new BigDecimal("100.00"), new BigDecimal("600.00"), "Test deposit", Instant.now());

        when(accountService.addMoneyToAccount(any())).thenReturn(txResult);
        when(transactionMapper.toDepositResponse(txResult)).thenReturn(depositResp);

        mockMvc.perform(post("/api/accounts/{accountId}/transactions/deposit", ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 100.00, \"description\": \"Test deposit\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void shouldReturn400_WhenDepositAmountIsZero() throws Exception {
        mockMvc.perform(post("/api/accounts/{accountId}/transactions/deposit", ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldWithdrawMoney_WhenRequestIsValid() throws Exception {
        var txResult     = new TransactionResultDTO(UUID.randomUUID(), "WITHDRAWAL",
                new CurrencyDTO("EUR", "Euro"), null,
                new BigDecimal("50.00"), new BigDecimal("450.00"), "Cash out", Instant.now());
        var withdrawResp = new WithdrawalResponse(UUID.randomUUID(), "WITHDRAWAL", "EUR",
                new BigDecimal("50.00"), new BigDecimal("450.00"), "Cash out", Instant.now());

        when(accountService.withdrawMoneyFromAccount(any())).thenReturn(txResult);
        when(transactionMapper.toWithdrawalResponse(txResult)).thenReturn(withdrawResp);

        mockMvc.perform(post("/api/accounts/{accountId}/transactions/withdraw", ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 50.00, \"description\": \"Cash out\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void shouldReturn422_WhenWithdrawalAccountNotFound() throws Exception {
        when(accountService.withdrawMoneyFromAccount(any()))
                .thenThrow(new AccountNotFoundException("Account not found"));

        mockMvc.perform(post("/api/accounts/{accountId}/transactions/withdraw", ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 50.00}"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void shouldReturn400_WhenExchangingWithinSameAccount() throws Exception {
        // sourceId (path) == destinationAccountId (body) → controller throws CurrencyExchangeWithinSameAccountException
        mockMvc.perform(post("/api/accounts/{accountId}/transactions/currency-exchange", ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationAccountId\": \"" + ACCOUNT_ID + "\", \"amountToTransfer\": 100.00}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldExchangeCurrency_WhenRequestIsValid() throws Exception {
        var destId      = UUID.randomUUID();
        var debitTx     = new TransactionResultDTO(UUID.randomUUID(), "EXCHANGE_OUT",
                new CurrencyDTO("EUR", "Euro"), new BigDecimal("1.1"),
                new BigDecimal("100.00"), new BigDecimal("400.00"), "FX", Instant.now());
        var creditTx    = new TransactionResultDTO(UUID.randomUUID(), "EXCHANGE_IN",
                new CurrencyDTO("USD", "US Dollar"), new BigDecimal("1.1"),
                new BigDecimal("110.00"), new BigDecimal("510.00"), "FX", Instant.now());
        var exchangeResult = ExchangeResultDTO.builder()
                .debitTransaction(debitTx)
                .creditTransaction(creditTx)
                .build();

        var exchangeResponse = ExchangeResponse.builder()
                .amountInSourceCurrency(new BigDecimal("100.00"))
                .amountInTargetCurrency(new BigDecimal("110.00"))
                .sourceCurrencyCode("EUR")
                .targetCurrencyCode("USD")
                .appliedRate(new BigDecimal("1.1"))
                .description("FX")
                .timestamp(Instant.now())
                .build();

        when(accountService.exchangeCurrency(any())).thenReturn(exchangeResult);
        when(transactionMapper.toExchangeResponse(exchangeResult)).thenReturn(exchangeResponse);

        mockMvc.perform(post("/api/accounts/{accountId}/transactions/currency-exchange", ACCOUNT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationAccountId\": \"" + destId + "\", \"amountToTransfer\": 100.00, \"description\": \"FX\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}

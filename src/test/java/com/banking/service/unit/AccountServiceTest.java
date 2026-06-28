package com.banking.service.unit;

import com.banking.service.constant.TransactionType;
import com.banking.service.dao.ExchangeRateDao;
import com.banking.service.entity.Account;
import com.banking.service.entity.Currency;
import com.banking.service.entity.User;
import com.banking.service.exception.AccountNotFoundException;
import com.banking.service.exception.InsufficientFundsException;
import com.banking.service.mapper.AccountMapper;
import com.banking.service.mapper.TransactionMapper;
import com.banking.service.repository.AccountRepository;
import com.banking.service.repository.TransactionRepository;
import com.banking.service.service.AccountService;
import com.banking.service.service.dto.AccountDTO;
import com.banking.service.service.dto.CurrencyDTO;
import com.banking.service.service.dto.TransactionRequestDTO;
import com.banking.service.service.dto.TransactionResultDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock AccountRepository accountRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock ExchangeRateDao exchangeRateDao;
    @Mock AccountMapper accountMapper;
    @Mock TransactionMapper transactionMapper;

    @InjectMocks
    AccountService accountService;

    private final Currency EUR = Currency.builder().code("EUR").name("Euro").build();
    private final Currency USD = Currency.builder().code("USD").name("US Dollar").build();
    private final User testUser = User.builder()
            .username("testuser").fullName("Test User").email("test@example.com").build();

    private Account buildAccount(Currency currency, BigDecimal balance) {
        return Account.builder()
                .accountNumber("ACC-" + UUID.randomUUID())
                .accountName("Test Account")
                .user(testUser)
                .currency(currency)
                .initialBalance(balance)
                .build();
    }

    @Test
    void shouldReturnAccountsByUserId_WhenUserHasAccounts() {
        var userId = UUID.randomUUID();
        var account = buildAccount(EUR, new BigDecimal("100.00"));
        var dto = new AccountDTO(UUID.randomUUID(), "ACC001", "Main", new BigDecimal("100.00"), "EUR");

        when(accountRepository.getByUserId(userId)).thenReturn(List.of(account));
        when(accountMapper.toDtoList(List.of(account))).thenReturn(List.of(dto));

        var result = accountService.getAccountsByUserId(userId);

        assertThat(result).hasSize(1).containsExactly(dto);
    }

    @Test
    void shouldReturnAccount_WhenAccountExists() {
        var accountId = UUID.randomUUID();
        var account = buildAccount(EUR, new BigDecimal("200.00"));
        var dto = new AccountDTO(accountId, "ACC001", "Main", new BigDecimal("200.00"), "EUR");

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountMapper.toDto(account)).thenReturn(dto);

        var result = accountService.getAccount(accountId);

        assertThat(result).isEqualTo(dto);
    }

    @Test
    void shouldThrowAccountNotFoundException_WhenAccountDoesNotExist() {
        var accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount(accountId))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void shouldReturnTransactionHistory_WhenAccountExists() {
        var accountId = UUID.randomUUID();
        when(accountRepository.existsById(accountId)).thenReturn(true);
        when(transactionRepository.findByAccountIdOrderByTimestampDesc(accountId)).thenReturn(List.of());

        var result = accountService.getTransactionHistory(accountId);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldThrowAccountNotFoundException_WhenGettingHistoryForNonExistentAccount() {
        var accountId = UUID.randomUUID();
        when(accountRepository.existsById(accountId)).thenReturn(false);

        assertThatThrownBy(() -> accountService.getTransactionHistory(accountId))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void shouldAddMoneyToAccount_WhenAccountExists() {
        var accountId = UUID.randomUUID();
        var account = buildAccount(EUR, new BigDecimal("500.00"));
        var request = TransactionRequestDTO.builder()
                .amount(new BigDecimal("100.00"))
                .destinationAccountId(accountId)
                .type(TransactionType.DEPOSIT)
                .build();
        var expected = new TransactionResultDTO(
                UUID.randomUUID(), "DEPOSIT",
                new CurrencyDTO("EUR", "Euro"),
                null, new BigDecimal("100.00"), new BigDecimal("600.00"), null, Instant.now());

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(transactionMapper.toTransactionResultDto(any())).thenReturn(expected);

        var result = accountService.addMoneyToAccount(request);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldThrowAccountNotFoundException_WhenDepositingToNonExistentAccount() {
        var accountId = UUID.randomUUID();
        var request = TransactionRequestDTO.builder()
                .amount(new BigDecimal("100.00"))
                .destinationAccountId(accountId)
                .type(TransactionType.DEPOSIT)
                .build();
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.addMoneyToAccount(request))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void shouldWithdrawMoneyFromAccount_WhenSufficientFunds() {
        var accountId = UUID.randomUUID();
        var account = buildAccount(EUR, new BigDecimal("1000.00"));
        var request = TransactionRequestDTO.builder()
                .amount(new BigDecimal("200.00"))
                .sourceAccountId(accountId)
                .type(TransactionType.WITHDRAWAL)
                .build();
        var expected = new TransactionResultDTO(
                UUID.randomUUID(), "WITHDRAWAL",
                new CurrencyDTO("EUR", "Euro"),
                null, new BigDecimal("200.00"), new BigDecimal("800.00"), null, Instant.now());

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(transactionMapper.toTransactionResultDto(any())).thenReturn(expected);

        var result = accountService.withdrawMoneyFromAccount(request);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldThrowInsufficientFundsException_WhenBalanceInsufficient() {
        var accountId = UUID.randomUUID();
        var account = buildAccount(EUR, new BigDecimal("50.00"));
        var request = TransactionRequestDTO.builder()
                .amount(new BigDecimal("100.00"))
                .sourceAccountId(accountId)
                .type(TransactionType.WITHDRAWAL)
                .build();
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.withdrawMoneyFromAccount(request))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void shouldExchangeCurrency_WhenValidAccountsAndRateExists() {
        var sourceId = UUID.randomUUID();
        var targetId = UUID.randomUUID();
        var sourceAccount = buildAccount(EUR, new BigDecimal("500.00"));
        var targetAccount = buildAccount(USD, new BigDecimal("400.00"));
        var request = TransactionRequestDTO.builder()
                .amount(new BigDecimal("100.00"))
                .sourceAccountId(sourceId)
                .destinationAccountId(targetId)
                .type(TransactionType.EXCHANGE_OUT)
                .build();
        var debitDto = new TransactionResultDTO(
                UUID.randomUUID(), "EXCHANGE_OUT", new CurrencyDTO("EUR", "Euro"),
                new BigDecimal("1.1"), new BigDecimal("100.00"), new BigDecimal("400.00"), null, Instant.now());
        var creditDto = new TransactionResultDTO(
                UUID.randomUUID(), "EXCHANGE_IN", new CurrencyDTO("USD", "US Dollar"),
                new BigDecimal("1.1"), new BigDecimal("110.00"), new BigDecimal("510.00"), null, Instant.now());

        when(accountRepository.findById(sourceId)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findById(targetId)).thenReturn(Optional.of(targetAccount));
        when(exchangeRateDao.getExchangeRate("EUR", "USD")).thenReturn(Optional.of(new BigDecimal("1.1")));
        when(transactionMapper.toTransactionResultDto(any())).thenReturn(debitDto, creditDto);

        var result = accountService.exchangeCurrency(request);

        assertThat(result.debitTransaction()).isEqualTo(debitDto);
        assertThat(result.creditTransaction()).isEqualTo(creditDto);
    }

    @Test
    void shouldThrowInsufficientFundsException_WhenExchangeBalanceInsufficient() {
        var sourceId = UUID.randomUUID();
        var targetId = UUID.randomUUID();
        var sourceAccount = buildAccount(EUR, new BigDecimal("50.00"));
        var request = TransactionRequestDTO.builder()
                .amount(new BigDecimal("100.00"))
                .sourceAccountId(sourceId)
                .destinationAccountId(targetId)
                .type(TransactionType.EXCHANGE_OUT)
                .build();
        when(accountRepository.findById(sourceId)).thenReturn(Optional.of(sourceAccount));

        assertThatThrownBy(() -> accountService.exchangeCurrency(request))
                .isInstanceOf(InsufficientFundsException.class);
    }
}

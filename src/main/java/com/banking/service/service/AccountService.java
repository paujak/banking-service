package com.banking.service.service;

import com.banking.service.constant.TransactionType;
import com.banking.service.dao.ExchangeRateDao;
import com.banking.service.entity.Account;
import com.banking.service.entity.Transaction;
import com.banking.service.exception.AccountConcurrentModificationException;
import com.banking.service.exception.AccountNotFoundException;
import com.banking.service.exception.InsufficientFundsException;
import com.banking.service.exception.NoExchangeRateDefined;
import com.banking.service.mapper.AccountMapper;
import com.banking.service.mapper.TransactionMapper;
import com.banking.service.repository.AccountRepository;
import com.banking.service.repository.TransactionRepository;
import com.banking.service.service.dto.AccountDTO;
import com.banking.service.service.dto.ExchangeResultDTO;
import com.banking.service.service.dto.TransactionRequestDTO;
import com.banking.service.service.dto.TransactionResultDTO;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final ExchangeRateDao exchangeRateDao;
    private final AccountMapper accountMapper;
    private final TransactionMapper transactionMapper;

    public AccountService(AccountRepository accountRepository,
                          TransactionRepository transactionRepository,
                          ExchangeRateDao exchangeRateDao,
                          AccountMapper accountMapper,
                          TransactionMapper transactionMapper) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.exchangeRateDao = exchangeRateDao;
        this.accountMapper = accountMapper;
        this.transactionMapper = transactionMapper;
    }

    @Transactional(readOnly = true)
    public List<AccountDTO> getAccountsByUserId(UUID userId) {
        List<Account> accounts = accountRepository.getByUserId(userId);
        return accountMapper.toDtoList(accounts);
    }

    @Transactional(readOnly = true)
    public AccountDTO getAccount(UUID accountId) {
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new AccountNotFoundException("Account not found"));
        return accountMapper.toDto(account);
    }

    @Transactional(readOnly = true)
    public List<TransactionResultDTO> getTransactionHistory(UUID accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException("Account not found");
        }
        return transactionRepository.findByAccountIdOrderByTimestampDesc(accountId)
                .stream()
                .map(transactionMapper::toTransactionResultDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionResultDTO> getTransactionHistory(UUID accountId, int page, int size) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException("Account not found");
        }
        return transactionRepository.findByAccountIdOrderByTimestampDesc(accountId, PageRequest.of(page, size))
                .stream()
                .map(transactionMapper::toTransactionResultDto)
                .toList();
    }

    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3,
               backoff = @Backoff(delay = 100, multiplier = 1.5))
    @Transactional
    public TransactionResultDTO addMoneyToAccount(TransactionRequestDTO transactionRequestDTO) {
        Account account = accountRepository.findById(transactionRequestDTO.destinationAccountId()).orElseThrow(() -> new AccountNotFoundException("Account not found"));
        var balanceAfter = account.getBalance().add(transactionRequestDTO.amount());
        account.setBalance(balanceAfter);
        Transaction transactionToStore = Transaction.builder()
                .account(account)
                .amount(transactionRequestDTO.amount())
                .balanceAfter(balanceAfter)
                .currency(account.getCurrency())
                .type(TransactionType.DEPOSIT)
                .description(transactionRequestDTO.description())
                .build();
        accountRepository.save(account);
        var storedTransaction = transactionRepository.save(transactionToStore);
        return transactionMapper.toTransactionResultDto(storedTransaction);
    }

    @Recover
    public TransactionResultDTO recoverTransaction(ObjectOptimisticLockingFailureException e, TransactionRequestDTO dto) {
        throw new AccountConcurrentModificationException("Account was modified concurrently; please retry");
    }

    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3,
               backoff = @Backoff(delay = 100, multiplier = 1.5))
    @Transactional
    public TransactionResultDTO withdrawMoneyFromAccount(TransactionRequestDTO transactionRequestDTO) {
        Account account = accountRepository.findById(transactionRequestDTO.sourceAccountId()).orElseThrow(() -> new AccountNotFoundException("Account not found"));
        if (account.getBalance().compareTo(transactionRequestDTO.amount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }
        var balanceAfter = account.getBalance().subtract(transactionRequestDTO.amount());
        account.setBalance(balanceAfter);
        Transaction transactionToStore = Transaction.builder()
                .account(account)
                .amount(transactionRequestDTO.amount())
                .balanceAfter(balanceAfter)
                .currency(account.getCurrency())
                .type(TransactionType.WITHDRAWAL)
                .description(transactionRequestDTO.description())
                .build();
        accountRepository.save(account);
        var storedTransaction = transactionRepository.save(transactionToStore);
        return transactionMapper.toTransactionResultDto(storedTransaction);
    }

    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3,
               backoff = @Backoff(delay = 100, multiplier = 1.5))
    @Transactional
    public ExchangeResultDTO exchangeCurrency(TransactionRequestDTO transactionRequestDTO) {
        if (transactionRequestDTO.sourceAccountId().equals(transactionRequestDTO.destinationAccountId())) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }
        Account sourceAccount = accountRepository.findById(transactionRequestDTO.sourceAccountId()).orElseThrow(() -> new AccountNotFoundException("Account not found"));
        if (sourceAccount.getBalance().compareTo(transactionRequestDTO.amount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }
        Account targetAccount = accountRepository.findById(transactionRequestDTO.destinationAccountId()).orElseThrow(() -> new AccountNotFoundException("Account not found"));
        BigDecimal exchangeRate = exchangeRateDao.getExchangeRate(sourceAccount.getCurrency().getCode(), targetAccount.getCurrency().getCode())
                .orElseThrow(() -> new NoExchangeRateDefined("Exchange rate not found: " + sourceAccount.getCurrency().getCode() + " to " + targetAccount.getCurrency().getCode()));
        BigDecimal amountToDebit = sourceAccount.getBalance().subtract(transactionRequestDTO.amount());
        BigDecimal amountToCredit = targetAccount.getBalance().add(transactionRequestDTO.amount().multiply(exchangeRate));
        sourceAccount.setBalance(amountToDebit);
        targetAccount.setBalance(amountToCredit);
        UUID correlationId = UUID.randomUUID();
        Transaction debitTransaction = Transaction.builder()
                .correlationId(correlationId)
                .account(sourceAccount)
                .amount(transactionRequestDTO.amount())
                .appliedRate(exchangeRate)
                .balanceAfter(amountToDebit)
                .currency(sourceAccount.getCurrency())
                .type(TransactionType.EXCHANGE_OUT)
                .description(transactionRequestDTO.description())
                .build();
        Transaction creditTransaction = Transaction.builder()
                .correlationId(correlationId)
                .account(targetAccount)
                .amount(transactionRequestDTO.amount().multiply(exchangeRate))
                .appliedRate(exchangeRate)
                .balanceAfter(amountToCredit)
                .currency(targetAccount.getCurrency())
                .type(TransactionType.EXCHANGE_IN)
                .description(transactionRequestDTO.description())
                .build();
        accountRepository.save(sourceAccount);
        accountRepository.save(targetAccount);
        transactionRepository.save(debitTransaction);
        transactionRepository.save(creditTransaction);
        return ExchangeResultDTO.builder()
                .debitTransaction(transactionMapper.toTransactionResultDto(debitTransaction))
                .creditTransaction(transactionMapper.toTransactionResultDto(creditTransaction))
                .build();
    }

    @Recover
    public ExchangeResultDTO recoverExchange(ObjectOptimisticLockingFailureException e, TransactionRequestDTO dto) {
        throw new AccountConcurrentModificationException("Account was modified concurrently; please retry");
    }
}

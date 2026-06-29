package com.banking.service.service;

import com.banking.service.constant.TransactionType;
import com.banking.service.dao.ExchangeRateDao;
import com.banking.service.entity.Account;
import com.banking.service.entity.Transaction;
import com.banking.service.exception.AccountConcurrentModificationException;
import com.banking.service.exception.AccountNotFoundException;
import com.banking.service.exception.CurrencyExchangeWithinSameAccountException;
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

/**
 * Business logic for account queries and financial transactions (deposits, withdrawals, currency exchange).
 * Write operations use optimistic locking with automatic retry on concurrent modification.
 */
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

    /**
     * Retrieves account details and current balance.
     *
     * @param accountId UUID of the account
     * @return account data as {@link AccountDTO}
     * @throws AccountNotFoundException if no account matches the given ID
     */
    @Transactional(readOnly = true)
    public AccountDTO getAccount(UUID accountId) {
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new AccountNotFoundException("Account not found"));
        return accountMapper.toDto(account);
    }

    /**
     * Returns the full transaction history for an account, ordered newest-first.
     *
     * @param accountId UUID of the account
     * @return ordered list of {@link TransactionResultDTO}
     * @throws AccountNotFoundException if no account matches the given ID
     */
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

    /**
     * Returns a paginated transaction history for an account, ordered newest-first.
     *
     * @param accountId UUID of the account
     * @param page      zero-based page index
     * @param size      number of results per page
     * @return ordered list of {@link TransactionResultDTO} for the requested page
     * @throws AccountNotFoundException if no account matches the given ID
     */
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

    /**
     * Credits money to an account and persists a DEPOSIT transaction.
     * Retried up to 3 times on optimistic locking conflicts.
     *
     * @param transactionRequestDTO contains destination account ID and deposit amount
     * @return result DTO with the created transaction and updated balance
     * @throws AccountNotFoundException if the destination account does not exist
     */
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

    /**
     * Recovery handler invoked when all deposit retries are exhausted.
     * Always throws {@link AccountConcurrentModificationException}.
     *
     * @param e   the optimistic locking exception that caused exhaustion
     * @param dto the original request DTO
     * @return never returns
     */
    @Recover
    public TransactionResultDTO recoverTransaction(ObjectOptimisticLockingFailureException e, TransactionRequestDTO dto) {
        throw new AccountConcurrentModificationException("Account was modified concurrently; please retry");
    }

    /**
     * Debits money from an account and persists a WITHDRAWAL transaction.
     * Retried up to 3 times on optimistic locking conflicts.
     *
     * @param transactionRequestDTO contains source account ID and withdrawal amount
     * @return result DTO with the created transaction and updated balance
     * @throws AccountNotFoundException   if the source account does not exist
     * @throws InsufficientFundsException if the account balance is below the requested amount
     */
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

    /**
     * Converts an amount from a source account's currency to a destination account's currency,
     * persisting an EXCHANGE_OUT transaction on the source and an EXCHANGE_IN transaction on the destination.
     * Retried up to 3 times on optimistic locking conflicts.
     *
     * @param transactionRequestDTO contains source and destination account IDs and the amount to transfer
     * @return {@link ExchangeResultDTO} holding both the debit and credit transaction results
     * @throws AccountNotFoundException                    if either account does not exist
     * @throws InsufficientFundsException                  if the source account balance is insufficient
     * @throws NoExchangeRateDefined                       if no rate is configured for the currency pair
     * @throws CurrencyExchangeWithinSameAccountException  if source and destination are the same account
     */
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3,
               backoff = @Backoff(delay = 100, multiplier = 1.5))
    @Transactional
    public ExchangeResultDTO exchangeCurrency(TransactionRequestDTO transactionRequestDTO) {
        if (transactionRequestDTO.sourceAccountId().equals(transactionRequestDTO.destinationAccountId())) {
            throw new CurrencyExchangeWithinSameAccountException("Source and destination accounts must be different");
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

    /**
     * Recovery handler invoked when all exchange retries are exhausted.
     * Always throws {@link AccountConcurrentModificationException}.
     *
     * @param e   the optimistic locking exception that caused exhaustion
     * @param dto the original request DTO
     * @return never returns
     */
    @Recover
    public ExchangeResultDTO recoverExchange(ObjectOptimisticLockingFailureException e, TransactionRequestDTO dto) {
        throw new AccountConcurrentModificationException("Account was modified concurrently; please retry");
    }
}

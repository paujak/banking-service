package com.banking.service.service;

import com.banking.service.constant.TransactionType;
import com.banking.service.dao.ExchangeRateDao;
import com.banking.service.entity.Account;
import com.banking.service.entity.Transaction;
import com.banking.service.exception.AccountNotFoundException;
import com.banking.service.exception.ExternalServiceException;
import com.banking.service.exception.InsufficientFundsException;
import com.banking.service.exception.NoExchangeRateDefined;
import com.banking.service.mapper.AccountMapper;
import com.banking.service.mapper.TransactionMapper;
import com.banking.service.repository.AccountRepository;
import com.banking.service.repository.TransactionRepository;
import com.banking.service.service.dto.AccountDTO;
import com.banking.service.service.dto.DebitStatusRequestDTO;
import com.banking.service.service.dto.ExchangeResultDTO;
import com.banking.service.service.dto.TransactionRequestDTO;
import com.banking.service.service.dto.TransactionResultDTO;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final ExchangeRateDao exchangeRateDao;
    private final AccountMapper accountMapper;
    private final TransactionMapper transactionMapper;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${banking-service.account.withdrawal.external-service.url}")
    private String debitCheckUrl;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public AccountService(AccountRepository accountRepository,
                          TransactionRepository transactionRepository,
                          ExchangeRateDao exchangeRateDao,
                          AccountMapper accountMapper,
                          TransactionMapper transactionMapper,
                          CloseableHttpClient httpClient,
                          ObjectMapper objectMapper) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.exchangeRateDao = exchangeRateDao;
        this.accountMapper = accountMapper;
        this.transactionMapper = transactionMapper;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
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

    @Transactional
    public TransactionResultDTO withdrawMoneyFromAccount(TransactionRequestDTO transactionRequestDTO) {
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

    @Transactional
    public ExchangeResultDTO exchangeCurrency(TransactionRequestDTO transactionRequestDTO) {
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

    private void logEvent(TransactionRequestDTO transactionRequestDTO, Account account) {
        BasicClassicHttpRequest request = new BasicClassicHttpRequest(Method.POST, debitCheckUrl);
        DebitStatusRequestDTO debitStatusRequestDTO = DebitStatusRequestDTO.builder()
                .userName(account.getUser().getUsername())
                .amount(transactionRequestDTO.amount())
                .accountId(account.getId())
                .build();
        String requestBody = objectMapper.writeValueAsString(debitStatusRequestDTO);
        request.setEntity(HttpEntities.create(requestBody, ContentType.APPLICATION_JSON));

        try {
            httpClient.execute(request, response -> {
                int statusCode = response.getCode();
                if (statusCode != 200) {
                    logger.error("External service call failed with status code: {}", statusCode);
                    throw new ExternalServiceException("External service call failed with status code: " + statusCode);
                }
                return null;
            });
        } catch (IOException e) {
            logger.error("Error occurred while calling external service", e);
            throw new ExternalServiceException(e.getMessage());
        if (transactionRequestDTO.sourceAccountId().equals(transactionRequestDTO.destinationAccountId())) {
            throw new CurrencyExchangeWithinSameAccountException("Source and destination accounts must be different");
        }
            BigDecimal amountToCredit = targetAccount.getBalance().add(transactionRequestDTO.amount().multiply(exchangeRate));
            sourceAccount.setBalance(amountToDebit);
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
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new AccountConcurrentModificationException("Account was modified concurrently; please retry");
        }
    }
}

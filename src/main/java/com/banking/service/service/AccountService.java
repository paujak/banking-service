package com.banking.service.service;

import com.banking.service.constant.TransactionType;
import com.banking.service.entity.Account;
import com.banking.service.entity.Transaction;
import com.banking.service.exception.AccountNotFoundException;
import com.banking.service.exception.ExternalServiceException;
import com.banking.service.exception.InsufficientFundsException;
import com.banking.service.mapper.AccountMapper;
import com.banking.service.mapper.TransactionMapper;
import com.banking.service.repository.AccountRepository;
import com.banking.service.repository.TransactionRepository;
import com.banking.service.service.dto.AccountDTO;
import com.banking.service.service.dto.DebitStatusRequestDTO;
import com.banking.service.service.dto.TransactionRequestDTO;
import com.banking.service.service.dto.TransactionResultDTO;
import java.io.IOException;
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
    private final AccountMapper accountMapper;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${banking-service.account.withdrawal.external-service.url}")
    private String debitCheckUrl;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public AccountService(AccountRepository accountRepository, 
                          AccountMapper accountMapper, 
                          TransactionRepository transactionRepository,
                          TransactionMapper transactionMapper, 
                          CloseableHttpClient httpClient, 
                          ObjectMapper objectMapper) {
        this.accountRepository = accountRepository;
        this.accountMapper = accountMapper;
        this.transactionRepository = transactionRepository;
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

    @Transactional
    public TransactionResultDTO addMoneyToAccount(TransactionRequestDTO transactionRequestDTO) throws AccountNotFoundException {
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
        var storedTransaction = transactionRepository.save(transactionToStore);
        return transactionMapper.toTransactionResultDto(storedTransaction);

    }

    @Transactional
    public TransactionResultDTO withdrawMoneyFromAccount(TransactionRequestDTO transactionRequestDTO) throws AccountNotFoundException, InsufficientFundsException {
        Account account = accountRepository.findById(transactionRequestDTO.sourceAccountId()).orElseThrow(() -> new AccountNotFoundException("Account not found"));
        if (account.getBalance().compareTo(transactionRequestDTO.amount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds for withdrawal");
        }
        logEvent(transactionRequestDTO, account);
        var balanceAfter = account.getBalance().subtract(transactionRequestDTO.amount());
        account.setBalance(balanceAfter);
        Transaction transactionToStore = Transaction.builder()
                .account(account)
                .amount(transactionRequestDTO.amount())
                .balanceAfter(balanceAfter)
                .currency(account.getCurrency())
                .type(TransactionType.WITHDRAWAL)
                .build();
        var storedTransaction = transactionRepository.save(transactionToStore);
        return transactionMapper.toTransactionResultDto(storedTransaction);
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
        }
    }
}

package com.banking.service.service;

import com.banking.service.constant.TransactionType;
import com.banking.service.entity.Account;
import com.banking.service.entity.Transaction;
import com.banking.service.exception.AccountNotFoundException;
import com.banking.service.exception.InsufficientFundsException;
import com.banking.service.mapper.AccountMapper;
import com.banking.service.mapper.TransactionMapper;
import com.banking.service.repository.AccountRepository;
import com.banking.service.repository.TransactionRepository;
import com.banking.service.service.dto.AccountDTO;
import com.banking.service.service.dto.TransactionRequestDTO;
import com.banking.service.service.dto.TransactionResultDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    public AccountService(AccountRepository accountRepository, AccountMapper accountMapper, TransactionRepository transactionRepository, TransactionMapper transactionMapper) {
        this.accountRepository = accountRepository;
        this.accountMapper = accountMapper;
        this.transactionRepository = transactionRepository;
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
        // TODO make a call to external service (https://tools-httpstatus.pickup-services.com/200 or Postman mockserver)
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
}

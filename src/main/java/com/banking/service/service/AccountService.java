package com.banking.service.service;

import com.banking.service.constant.TransactionType;
import com.banking.service.controller.WithdrawalRequestDTO;
import com.banking.service.dto.AccountResponseDTO;
import com.banking.service.dto.DepositRequestDTO;
import com.banking.service.dto.DepositResponseDTO;
import com.banking.service.dto.WithdrawalResponseDTO;
import com.banking.service.entity.Transaction;
import com.banking.service.exception.AccountNotFoundException;
import com.banking.service.exception.InsufficientFundsException;
import com.banking.service.mapper.AccountMapper;
import com.banking.service.repository.AccountRepository;
import com.banking.service.repository.TransactionRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final TransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository, AccountMapper accountMapper, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.accountMapper = accountMapper;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public List<AccountResponseDTO> getAccountsByUserId(UUID userId) {
        var accounts = accountRepository.getByUserId(userId);
        return accountMapper.toDtoList(accounts);
    }

    @Transactional(readOnly = true)
    public AccountResponseDTO getAccount(UUID accountId) {
        var account = accountRepository.findById(accountId).orElseThrow(() -> new AccountNotFoundException("Account not found"));
        return accountMapper.toDto(account);
    }

    @Transactional
    public DepositResponseDTO addMoneyToAccount(UUID accountId, DepositRequestDTO depositRequestDTO) throws AccountNotFoundException {
        var account = accountRepository.findById(accountId).orElseThrow(() -> new AccountNotFoundException("Account not found"));
        var balanceAfter = account.getBalance().add(depositRequestDTO.amount());
        account.setBalance(balanceAfter);
        var transactionToStore = Transaction.builder()
                .account(account)
                .amount(depositRequestDTO.amount())
                .balanceAfter(balanceAfter)
                .currency(account.getCurrency())
                .type(TransactionType.DEPOSIT)
                .build();
        var storedTransaction = transactionRepository.save(transactionToStore);
        return DepositResponseDTO.builder()
                .transactionId(storedTransaction.getId())
                .amount(storedTransaction.getAmount())
                .balanceAfter(storedTransaction.getBalanceAfter())
                .currencyCode(storedTransaction.getCurrency().getCode())
                .type(storedTransaction.getType().name())
                .timestamp(storedTransaction.getTimestamp())
                .build();
        
    }

    @Transactional
    public WithdrawalResponseDTO withdrawMoneyFromAccount(UUID accountId, @Valid WithdrawalRequestDTO withdrawalRequestDTO) throws AccountNotFoundException, InsufficientFundsException {
        var account = accountRepository.findById(accountId).orElseThrow(() -> new AccountNotFoundException("Account not found"));
        if (account.getBalance().compareTo(withdrawalRequestDTO.amount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds for withdrawal");
        }
        var balanceAfter = account.getBalance().subtract(withdrawalRequestDTO.amount());
        account.setBalance(balanceAfter);
        var transactionToStore = Transaction.builder()
                .account(account)
                .amount(withdrawalRequestDTO.amount())
                .balanceAfter(balanceAfter)
                .currency(account.getCurrency())
                .type(TransactionType.WITHDRAWAL)
                .build();
        var storedTransaction = transactionRepository.save(transactionToStore);
        return WithdrawalResponseDTO.builder()
                .transactionId(storedTransaction.getId())
                .amount(storedTransaction.getAmount())
                .balanceAfter(storedTransaction.getBalanceAfter())
                .currencyCode(storedTransaction.getCurrency().getCode())
                .type(storedTransaction.getType().name())
                .timestamp(storedTransaction.getTimestamp())
                .build();
    }
}

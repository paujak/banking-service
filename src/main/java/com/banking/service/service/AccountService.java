package com.banking.service.service;

import com.banking.service.constant.TransactionType;
import com.banking.service.dto.AccountResponseDTO;
import com.banking.service.dto.DepositRequestDTO;
import com.banking.service.entity.Transaction;
import com.banking.service.exception.AccountNotFoundException;
import com.banking.service.mapper.AccountMapper;
import com.banking.service.repository.AccountRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
    
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    
    public AccountService(AccountRepository accountRepository, AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.accountMapper = accountMapper;
    }
    
    @Transactional(readOnly = true)
    public List<AccountResponseDTO> getAccountsByUserId(UUID userId) {
        var accounts = accountRepository.getByUserId(userId);
        return accountMapper.toDtoList(accounts);
    }

    @Transactional
    public Transaction addMoneyToAccount(UUID accountId, DepositRequestDTO depositRequestDTO) throws AccountNotFoundException {
        var account = accountRepository.findById(accountId).orElseThrow(() -> new AccountNotFoundException("Account not found"));
        var balanceAfter = account.getBalance().add(depositRequestDTO.amount());
        account.setBalance(balanceAfter);
        return Transaction.builder()
                .account(account)
                .amount(depositRequestDTO.amount())
                .balanceAfter(balanceAfter)
                .currency(account.getCurrency())
                .type(TransactionType.DEPOSIT)
                .build();
    }
}

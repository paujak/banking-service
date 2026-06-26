package com.banking.service.service;

import com.banking.service.dao.AccountDao;
import com.banking.service.dto.AccountResponseDTO;
import com.banking.service.mapper.AccountMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
    
    private final AccountDao accountDao;
    private final AccountMapper accountMapper;
    
    public AccountService(AccountDao accountDao, AccountMapper accountMapper) {
        this.accountDao = accountDao;
        this.accountMapper = accountMapper;
    }
    
    @Transactional(readOnly = true)
    public List<AccountResponseDTO> getAccountsByUserId(UUID userId) {
        var accounts = accountDao.getByUserId(userId);
        return accountMapper.toDtoList(accounts);
    }
}

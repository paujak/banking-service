package com.banking.service.service;

import com.banking.service.dao.AccountDao;
import com.banking.service.entity.Account;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
    
    private final AccountDao accountDao;
    
    public AccountService(AccountDao accountDao) {
        this.accountDao = accountDao;
    }
    
    public List<Account> getByUserId(UUID userId) {
        accountDao.getByUserId(userId);
    }
}

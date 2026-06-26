package com.banking.service.dao.impl;

import com.banking.service.dao.AccountDao;
import com.banking.service.entity.Account;
import com.banking.service.repository.AccountRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JpaAccountDaoImpl implements AccountDao {
    
    private final AccountRepository accountRepository;
    
    public JpaAccountDaoImpl(@Autowired AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }
    
    @Override
    public List<Account> getByUserId(UUID userId) {
        return accountRepository.getByUserId(userId);
    }
}

package com.banking.service.dao;

import com.banking.service.entity.Account;
import java.util.List;
import java.util.UUID;

public interface AccountDao {
    List<Account> getByUserId(UUID userId);
}

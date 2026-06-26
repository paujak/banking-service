package com.banking.service.repository;

import com.banking.service.entity.Account;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    List<Account> getByUserId(UUID userId);

}

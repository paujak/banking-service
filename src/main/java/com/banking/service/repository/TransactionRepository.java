package com.banking.service.repository;

import com.banking.service.entity.Transaction;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByAccountIdOrderByTimestampDesc(UUID accountId);
    List<Transaction> findByAccountIdOrderByTimestampDesc(UUID accountId, Pageable pageable);
    long countByAccountId(UUID accountId);
}

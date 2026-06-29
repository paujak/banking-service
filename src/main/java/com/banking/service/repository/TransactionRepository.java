package com.banking.service.repository;

import com.banking.service.entity.Transaction;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link Transaction} entities.
 */
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Returns all transactions for a given account, ordered newest-first.
     *
     * @param accountId UUID of the account
     * @return transactions ordered by timestamp descending
     */
    List<Transaction> findByAccountIdOrderByTimestampDesc(UUID accountId);

    /**
     * Returns a page of transactions for a given account, ordered newest-first.
     *
     * @param accountId UUID of the account
     * @param pageable  pagination settings (page index and page size)
     * @return transactions for the requested page, ordered by timestamp descending
     */
    List<Transaction> findByAccountIdOrderByTimestampDesc(UUID accountId, Pageable pageable);

    /**
     * Counts the total number of transactions for a given account.
     *
     * @param accountId UUID of the account
     * @return total transaction count
     */
    long countByAccountId(UUID accountId);
}

package com.banking.service.repository;

import com.banking.service.entity.Account;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link Account} entities.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    /**
     * Returns all accounts associated with the given user.
     *
     * @param userId UUID of the owning user
     * @return list of accounts; empty if the user has none
     */
    List<Account> getByUserId(UUID userId);
}

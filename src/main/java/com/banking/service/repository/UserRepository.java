package com.banking.service.repository;

import com.banking.service.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link User} entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their unique email address.
     *
     * @param email the user's email
     * @return an {@link Optional} containing the user, or empty if not found
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds a user by their unique username.
     *
     * @param username the user's username
     * @return an {@link Optional} containing the user, or empty if not found
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a user by their ID.
     *
     * @param userId UUID of the user
     * @return an {@link Optional} containing the user, or empty if not found
     */
    Optional<User> findFirstById(UUID userId);
}

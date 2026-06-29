package com.banking.service.service;

import com.banking.service.entity.Account;
import com.banking.service.entity.User;
import com.banking.service.exception.UserIsNotFoundException;
import com.banking.service.mapper.AccountMapper;
import com.banking.service.repository.UserRepository;
import com.banking.service.service.dto.AccountDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for user-related queries.
 */
@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final AccountMapper accountMapper;
    
    public UserService(UserRepository userRepository,
                       AccountMapper accountMapper) {
        this.userRepository = userRepository;
        this.accountMapper = accountMapper;
    }
    
    /**
     * Returns all accounts belonging to the specified user.
     *
     * @param userId UUID of the user
     * @return list of {@link AccountDTO}; empty if the user has no accounts
     * @throws UserIsNotFoundException if no user matches the given ID
     */
    @Transactional(readOnly = true)
    public List<AccountDTO> getUserAccounts(UUID userId) {
        List<Account> accounts = userRepository.findFirstById(userId).map(User::getAccounts)
                .orElseThrow(() -> new UserIsNotFoundException("User not found with id: " + userId));
        return accountMapper.toDtoList(accounts);
    }
}

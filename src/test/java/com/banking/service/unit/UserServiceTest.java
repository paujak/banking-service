package com.banking.service.unit;

import com.banking.service.entity.Account;
import com.banking.service.entity.Currency;
import com.banking.service.entity.User;
import com.banking.service.exception.UserIsNotFoundException;
import com.banking.service.mapper.AccountMapper;
import com.banking.service.repository.UserRepository;
import com.banking.service.service.UserService;
import com.banking.service.service.dto.AccountDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock AccountMapper accountMapper;

    @InjectMocks
    UserService userService;

    @Test
    void shouldReturnAccountsForUser_WhenUserExists() {
        var userId  = UUID.randomUUID();
        var user    = mock(User.class);
        var account = Account.builder()
                .accountNumber("ACC-001")
                .accountName("Main")
                .currency(Currency.builder().code("EUR").name("Euro").build())
                .initialBalance(new BigDecimal("100.00"))
                .build();
        var dto = new AccountDTO(UUID.randomUUID(), "ACC-001", "Main", new BigDecimal("100.00"), "EUR");

        when(userRepository.findFirstById(userId)).thenReturn(Optional.of(user));
        when(user.getAccounts()).thenReturn(List.of(account));
        when(accountMapper.toDtoList(List.of(account))).thenReturn(List.of(dto));

        var result = userService.getUserAccounts(userId);

        assertThat(result).hasSize(1).containsExactly(dto);
    }

    @Test
    void shouldThrowUserIsNotFoundException_WhenUserDoesNotExist() {
        var userId = UUID.randomUUID();
        when(userRepository.findFirstById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserAccounts(userId))
                .isInstanceOf(UserIsNotFoundException.class);
    }
}

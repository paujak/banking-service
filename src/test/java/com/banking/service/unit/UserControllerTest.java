package com.banking.service.unit;

import com.banking.service.controller.UserController;
import com.banking.service.controller.dto.AccountResponse;
import com.banking.service.exception.GlobalExceptionHandler;
import com.banking.service.exception.UserIsNotFoundException;
import com.banking.service.mapper.AccountMapper;
import com.banking.service.service.UserService;
import com.banking.service.service.dto.AccountDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock UserService userService;
    @Mock AccountMapper accountMapper;

    MockMvc mockMvc;

    private static final UUID USER_ID    = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ACCOUNT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        var controller = new UserController(userService, accountMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnAccounts_WhenUserHasAccounts() throws Exception {
        var dto      = new AccountDTO(ACCOUNT_ID, "ACC001", "Main", new BigDecimal("100.00"), "EUR");
        var response = new AccountResponse(ACCOUNT_ID, "ACC001", "Main", new BigDecimal("100.00"), "EUR");
        when(userService.getUserAccounts(USER_ID)).thenReturn(List.of(dto));
        when(accountMapper.toResponseList(List.of(dto))).thenReturn(List.of(response));

        mockMvc.perform(get("/api/users/{userId}/accounts", USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void shouldReturnEmptyList_WhenUserHasNoAccounts() throws Exception {
        when(userService.getUserAccounts(USER_ID)).thenReturn(List.of());
        when(accountMapper.toResponseList(List.of())).thenReturn(List.of());

        mockMvc.perform(get("/api/users/{userId}/accounts", USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[]"));
    }

    @Test
    void shouldReturn404_WhenUserNotFound() throws Exception {
        when(userService.getUserAccounts(USER_ID))
                .thenThrow(new UserIsNotFoundException("User not found with id: " + USER_ID));

        mockMvc.perform(get("/api/users/{userId}/accounts", USER_ID))
                .andExpect(status().isNotFound());
    }
}

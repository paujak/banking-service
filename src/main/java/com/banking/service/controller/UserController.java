package com.banking.service.controller;

import com.banking.service.controller.dto.AccountResponse;
import com.banking.service.mapper.AccountMapper;
import com.banking.service.service.UserService;
import com.banking.service.service.dto.AccountDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}")
public class UserController {
    
    private final UserService userService;
    private final AccountMapper accountMapper;
    
    public UserController(UserService userService,
                          AccountMapper accountMapper) {
        this.userService = userService;
        this.accountMapper = accountMapper;
    }

    @GetMapping(value = "/accounts", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AccountResponse>> getAccountsOfUser(@PathVariable UUID userId) {
        List<AccountDTO> accounts = userService.getUserAccounts(userId);
        return ResponseEntity.ok(accountMapper.toResponseList(accounts));
    }
}

package com.banking.service.controller;

import com.banking.service.entity.Account;
import com.banking.service.service.AccountService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }
    
    @GetMapping(value = "/api/users/{userId}/accounts", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Account> getAccountsOfUser(@PathVariable UUID userId) {
        return accountService.getByUserId(userId);
    }
}

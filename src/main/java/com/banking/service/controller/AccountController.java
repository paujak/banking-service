package com.banking.service.controller;

import com.banking.service.dto.AccountResponseDTO;
import com.banking.service.dto.DepositRequestDTO;
import com.banking.service.dto.DepositResponseDTO;
import com.banking.service.exception.AccountNotFoundException;
import com.banking.service.service.AccountService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AccountController {
    
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }
    
    @GetMapping(value = "/users/{userId}/accounts", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AccountResponseDTO>> getAccountsOfUser(@PathVariable UUID userId) {
        List<AccountResponseDTO> accounts = accountService.getAccountsByUserId(userId);
        return ResponseEntity.ok(accounts);
    }
    
    @PostMapping(value = "/accounts/{accountId}/transactions/deposit", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DepositResponseDTO> addMoneyToAccount(@PathVariable UUID accountId, @Valid @RequestBody DepositRequestDTO depositRequestDTO) throws AccountNotFoundException {
        accountService.addMoneyToAccount(accountId, depositRequestDTO);
        return ResponseEntity.ok().build();
    }
}

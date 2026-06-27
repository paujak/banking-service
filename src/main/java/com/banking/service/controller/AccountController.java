package com.banking.service.controller;

import com.banking.service.dto.AccountResponseDTO;
import com.banking.service.dto.DepositRequestDTO;
import com.banking.service.dto.DepositResponseDTO;
import com.banking.service.dto.WithdrawResponseDTO;
import com.banking.service.exception.AccountNotFoundException;
import com.banking.service.exception.InsufficientFundsException;
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
        var transaction = accountService.addMoneyToAccount(accountId, depositRequestDTO);
        return ResponseEntity
                .ok()
                .body(DepositResponseDTO.builder()
                        .transactionId(transaction.getId())
                        .type(transaction.getType().name())
                        .amountDeposited(transaction.getAmount())
                        .currencyCode(transaction.getCurrency().getCode())
                        .balanceAfter(transaction.getBalanceAfter())
                        .description(transaction.getDescription())
                        .timestamp(transaction.getTimestamp())
                .build());
    }
    
    @PostMapping(value = "/accounts/{accountId}/transactions/withdraw", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WithdrawResponseDTO> withdrawMoneyFromAccount(@PathVariable UUID accountId, @Valid @RequestBody WithdrawRequestDTO withdrawRequestDTO) throws AccountNotFoundException, InsufficientFundsException {
        var transaction = accountService.withdrawMoneyFromAccount(accountId, withdrawRequestDTO);
        return ResponseEntity
                .ok()
                .body(WithdrawResponseDTO.builder()
                        .transactionId(transaction.getId())
                        .type(transaction.getType().name())
                        .amountWithdrawn(transaction.getAmount())
                        .currencyCode(transaction.getCurrency().getCode())
                        .balanceAfter(transaction.getBalanceAfter())
                        .description(transaction.getDescription())
                        .timestamp(transaction.getTimestamp())
                .build());
    }
}

package com.banking.service.controller;

import com.banking.service.dto.AccountResponseDTO;
import com.banking.service.dto.DepositRequestDTO;
import com.banking.service.dto.DepositResponseDTO;
import com.banking.service.dto.ExchangeRequestDTO;
import com.banking.service.dto.WithdrawalRequestDTO;
import com.banking.service.dto.WithdrawalResponseDTO;
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
    
    
    // TODO move this to the User controller and use UserService/UserRepository to retrieve accounts for the user; then change request mapping
    @GetMapping(value = "/users/{userId}/accounts", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AccountResponseDTO>> getAccountsOfUser(@PathVariable UUID userId) {
        List<AccountResponseDTO> accounts = accountService.getAccountsByUserId(userId);
        return ResponseEntity.ok(accounts);
    }
    
    // TODO introduce response and request entity records for all endpoints to separate view from business logic 
    
    @GetMapping(value = "/accounts/{accountId}/balance", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountResponseDTO> getAccount(@PathVariable UUID accountId) throws AccountNotFoundException {
        var accountResponseDTO = accountService.getAccount(accountId);
        return ResponseEntity.ok().body(accountResponseDTO);
    }
    
    @PostMapping(value = "/accounts/{accountId}/transactions/deposit", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DepositResponseDTO> addMoneyToAccount(@PathVariable UUID accountId, @Valid @RequestBody DepositRequestDTO depositRequestDTO) throws AccountNotFoundException {
        var depositResponseDTO = accountService.addMoneyToAccount(accountId, depositRequestDTO);
        return ResponseEntity
                .ok()
                .body(depositResponseDTO);
    }
    
    @PostMapping(value = "/accounts/{accountId}/transactions/withdraw", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WithdrawalResponseDTO> withdrawMoneyFromAccount(@PathVariable UUID accountId, @Valid @RequestBody WithdrawalRequestDTO withdrawalRequestDTO) throws AccountNotFoundException, InsufficientFundsException {
        // TODO make a call to external service (https://tools-httpstatus.pickup-services.com/200 or Postman mockserver)
        var withdrawalResponseDTO = accountService.withdrawMoneyFromAccount(accountId, withdrawalRequestDTO);
        return ResponseEntity
                .ok()
                .body(withdrawalResponseDTO);
                
    }
    
    @PostMapping(value = "/accounts/{accountId}/transactions/currency-exchange", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ExchangeResponseDTO> exchangeCurrency(@PathVariable UUID accountId, @Valid @RequestBody ExchangeRequestDTO exchangeRequestDTO) throws AccountNotFoundException, InsufficientFundsException {
        var exchangeResponseDTO = accountService.exchangeCurrency(accountId, exchangeRequestDTO);
        return ResponseEntity
                .ok()
                .body(exchangeResponseDTO);
    }
    
}

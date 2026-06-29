package com.banking.service.controller;

import com.banking.service.constant.TransactionType;
import com.banking.service.controller.dto.AccountResponse;
import com.banking.service.controller.dto.DepositRequest;
import com.banking.service.controller.dto.DepositResponse;
import com.banking.service.controller.dto.ExchangeRequest;
import com.banking.service.controller.dto.ExchangeResponse;
import com.banking.service.controller.dto.TransactionResponse;
import com.banking.service.controller.dto.WithdrawalRequest;
import com.banking.service.controller.dto.WithdrawalResponse;
import com.banking.service.exception.AccountNotFoundException;
import com.banking.service.exception.CurrencyExchangeWithinSameAccountException;
import com.banking.service.mapper.AccountMapper;
import com.banking.service.mapper.TransactionMapper;
import com.banking.service.service.AccountService;
import com.banking.service.service.ExternalLoggingService;
import com.banking.service.service.dto.AccountDTO;
import com.banking.service.service.dto.ExchangeResultDTO;
import com.banking.service.service.dto.TransactionRequestDTO;
import com.banking.service.service.dto.TransactionResultDTO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AccountController {

    private final AccountService accountService;
    private final AccountMapper accountMapper;
    private final TransactionMapper transactionMapper;
    private final ExternalLoggingService externalLoggingService;

    public AccountController(AccountService accountService,
                             AccountMapper accountMapper,
                             TransactionMapper transactionMapper,
                             ExternalLoggingService externalLoggingService) {
        this.accountService = accountService;
        this.accountMapper = accountMapper;
        this.transactionMapper = transactionMapper;
        this.externalLoggingService = externalLoggingService;
    }

    @GetMapping(value = "/accounts/{accountId}/balance", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID accountId) throws AccountNotFoundException {
        AccountDTO account = accountService.getAccount(accountId);
        return ResponseEntity.ok().body(accountMapper.toResponse(account));
    }

    @GetMapping(value = "/accounts/{accountId}/transactions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<TransactionResultDTO> transactions = accountService.getTransactionHistory(accountId, page, size);
        return ResponseEntity.ok(transactions.stream().map(transactionMapper::toTransactionResponse).toList());
    }

    @PostMapping(value = "/accounts/{accountId}/transactions/deposit", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DepositResponse> addMoneyToAccount(@PathVariable UUID accountId, @Valid @RequestBody DepositRequest depositRequest) {
        TransactionRequestDTO transactionRequestDTO = TransactionRequestDTO.builder()
                .amount(depositRequest.amount())
                .description(depositRequest.description())
                .destinationAccountId(accountId)
                .type(TransactionType.DEPOSIT)
                .build();
        TransactionResultDTO transactionResultDTO = accountService.addMoneyToAccount(transactionRequestDTO);
        return ResponseEntity
                .ok()
                .body(transactionMapper.toDepositResponse(transactionResultDTO));
    }

    @PostMapping(value = "/accounts/{accountId}/transactions/withdraw", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WithdrawalResponse> withdrawMoneyFromAccount(@PathVariable UUID accountId, @Valid @RequestBody WithdrawalRequest withdrawalRequest) {
        externalLoggingService.notifyWithdrawal(accountId, withdrawalRequest.amount());
        TransactionRequestDTO transactionRequestDTO = TransactionRequestDTO.builder()
                .amount(withdrawalRequest.amount())
                .description(withdrawalRequest.description())
                .sourceAccountId(accountId)
                .type(TransactionType.WITHDRAWAL)
                .build();
        TransactionResultDTO transactionResultDTO = accountService.withdrawMoneyFromAccount(transactionRequestDTO);
        return ResponseEntity
                .ok()
                .body(transactionMapper.toWithdrawalResponse(transactionResultDTO));

    }

    @PostMapping(value = "/accounts/{accountId}/transactions/currency-exchange", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ExchangeResponse> exchangeCurrency(@PathVariable UUID accountId, @Valid @RequestBody ExchangeRequest exchangeRequest) {
        if (accountId.equals(exchangeRequest.destinationAccountId())) {
            throw new CurrencyExchangeWithinSameAccountException("Source and destination accounts must be different");
        }
        TransactionRequestDTO transactionRequestDTO = TransactionRequestDTO.builder()
                .amount(exchangeRequest.amountToTransfer())
                .description(exchangeRequest.description())
                .sourceAccountId(accountId)
                .destinationAccountId(exchangeRequest.destinationAccountId())
                .type(TransactionType.EXCHANGE_OUT)
                .build();
        ExchangeResultDTO exchangeResultDTO = accountService.exchangeCurrency(transactionRequestDTO);
        return ResponseEntity
                .ok()
                .body(transactionMapper.toExchangeResponse(exchangeResultDTO));
    }

}

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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 * REST controller for account-scoped operations.
 * All endpoints are rooted at {@code /api/accounts/{accountId}}.
 */
@Tag(name = "Accounts", description = "Account balance, transaction history, deposits, withdrawals and currency exchange")
@RestController
@RequestMapping("/api/accounts/{accountId}")
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

    /**
     * Returns details and current balance for the specified account.
     *
     * @param accountId UUID of the account to retrieve
     * @return 200 with {@link AccountResponse}, or 422 if the account does not exist
     * @throws AccountNotFoundException if no account matches the given ID
     */
    @Operation(summary = "Get account details", description = "Returns current balance and metadata for the given account")
    @ApiResponse(responseCode = "200", description = "Account found")
    @ApiResponse(responseCode = "422", description = "Account not found")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountResponse> getAccount(
            @Parameter(description = "UUID of the account — obtain from GET /api/users/{userId}/accounts")
            @PathVariable UUID accountId) throws AccountNotFoundException {
        AccountDTO account = accountService.getAccount(accountId);
        return ResponseEntity.ok().body(accountMapper.toResponse(account));
    }

    /**
     * Returns a paginated list of transactions for the specified account, ordered newest-first.
     *
     * @param accountId UUID of the account
     * @param page      zero-based page index (default 0)
     * @param size      number of transactions per page (default 20)
     * @return 200 with the list of {@link TransactionResponse}
     */
    @Operation(summary = "Get transaction history", description = "Returns paginated transactions for the account, ordered newest-first")
    @ApiResponse(responseCode = "200", description = "Transaction list returned")
    @ApiResponse(responseCode = "422", description = "Account not found")
    @GetMapping(value = "/transactions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory(
            @Parameter(description = "UUID of the account — obtain from GET /api/users/{userId}/accounts")
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<TransactionResultDTO> transactions = accountService.getTransactionHistory(accountId, page, size);
        return ResponseEntity.ok(transactions.stream().map(transactionMapper::toTransactionResponse).toList());
    }

    /**
     * Deposits money into the specified account.
     *
     * @param accountId      UUID of the destination account
     * @param depositRequest deposit amount and optional description
     * @return 200 with {@link DepositResponse} on success
     */
    @Operation(summary = "Deposit money", description = "Credits the given amount to the account and records a DEPOSIT transaction")
    @ApiResponse(responseCode = "200", description = "Deposit successful")
    @ApiResponse(responseCode = "400", description = "Invalid request (e.g. amount ≤ 0)")
    @ApiResponse(responseCode = "422", description = "Account not found")
    @PostMapping(value = "/transactions/deposit", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DepositResponse> addMoneyToAccount(
            @Parameter(description = "UUID of the account — obtain from GET /api/users/{userId}/accounts")
            @PathVariable UUID accountId, @Valid @RequestBody DepositRequest depositRequest) {
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

    /**
     * Withdraws money from the specified account.
     * Notifies the external debit-check service before processing the withdrawal.
     *
     * @param accountId         UUID of the source account
     * @param withdrawalRequest withdrawal amount and optional description
     * @return 200 with {@link WithdrawalResponse} on success
     */
    @Operation(summary = "Withdraw money", description = "Debits the given amount from the account, notifies the external debit-check service, and records a WITHDRAWAL transaction")
    @ApiResponse(responseCode = "200", description = "Withdrawal successful")
    @ApiResponse(responseCode = "400", description = "Insufficient funds or invalid request")
    @ApiResponse(responseCode = "422", description = "Account not found")
    @PostMapping(value = "/transactions/withdraw", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WithdrawalResponse> withdrawMoneyFromAccount(
            @Parameter(description = "UUID of the account — obtain from GET /api/users/{userId}/accounts")
            @PathVariable UUID accountId, @Valid @RequestBody WithdrawalRequest withdrawalRequest) {
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

    /**
     * Exchanges currency between two accounts. Source and destination accounts must differ.
     *
     * @param accountId       UUID of the source (debit) account
     * @param exchangeRequest destination account ID, amount to transfer, and optional description
     * @return 200 with {@link ExchangeResponse} on success
     * @throws CurrencyExchangeWithinSameAccountException if source and destination are the same account
     */
    @Operation(summary = "Exchange currency", description = "Transfers an amount from the source account to a destination account, applying the configured exchange rate")
    @ApiResponse(responseCode = "200", description = "Exchange successful")
    @ApiResponse(responseCode = "400", description = "Insufficient funds, same-account exchange, or invalid request")
    @ApiResponse(responseCode = "422", description = "Source or destination account not found")
    @ApiResponse(responseCode = "500", description = "No exchange rate configured for the currency pair")
    @PostMapping(value = "/transactions/currency-exchange", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ExchangeResponse> exchangeCurrency(
            @Parameter(description = "UUID of the source (debit) account — obtain from GET /api/users/{userId}/accounts")
            @PathVariable UUID accountId, @Valid @RequestBody ExchangeRequest exchangeRequest) {
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

package com.banking.service.controller;

import com.banking.service.controller.dto.AccountResponse;
import com.banking.service.mapper.AccountMapper;
import com.banking.service.service.UserService;
import com.banking.service.service.dto.AccountDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for user-scoped operations.
 * All endpoints are rooted at {@code /api/users/{userId}}.
 */
@Tag(name = "Users", description = "User account management")
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

    /**
     * Returns all accounts belonging to the specified user.
     *
     * @param userId UUID of the user
     * @return 200 with a list of {@link AccountResponse}, or 404 if the user does not exist
     */
    @Operation(
            summary = "Get user accounts",
            description = "Returns all accounts owned by the given user. Use the demo user ID `cfb095a4-bed2-4aa4-93c6-abf73d2dbbe1` (John Doe) to explore pre-seeded data.")
    @ApiResponse(responseCode = "200", description = "Accounts returned (empty list if user has none)")
    @ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping(value = "/accounts", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AccountResponse>> getAccountsOfUser(
            @Parameter(description = "UUID of the user", example = "cfb095a4-bed2-4aa4-93c6-abf73d2dbbe1")
            @PathVariable UUID userId) {
        List<AccountDTO> accounts = userService.getUserAccounts(userId);
        return ResponseEntity.ok(accountMapper.toResponseList(accounts));
    }
}

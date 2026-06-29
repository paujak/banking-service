package com.banking.service.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request body for initiating a currency exchange between two accounts.
 * Unknown JSON fields are silently ignored.
 *
 * @param destinationAccountId UUID of the account to credit; must differ from the source account
 * @param amountToTransfer     amount to deduct from the source account; must be greater than zero
 * @param description          optional free-text note; max 255 characters
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExchangeRequest(
        @NotNull
        UUID destinationAccountId,
        @NotNull
        @DecimalMin(value = "0.00", inclusive = false, message = "Amount must be greater than zero")
        BigDecimal amountToTransfer,
        @Nullable
        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description
) {
}

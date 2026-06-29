package com.banking.service.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Request body for depositing money into an account.
 * Unknown JSON fields are silently ignored.
 *
 * @param amount      deposit amount; must be greater than zero
 * @param description optional free-text note; max 255 characters
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DepositRequest(
        @NotNull
        @DecimalMin(value = "0.00", inclusive = false, message = "Amount must be greater than zero")
        BigDecimal amount,
        @Nullable
        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description
    )
{}

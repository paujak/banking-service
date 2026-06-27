package com.banking.service.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record WithdrawalRequestDTO(
        @NotNull
        @DecimalMin(value = "0.00", inclusive = false, message = "Amount must be greater than zero")
        BigDecimal amount,
        @Nullable
        @Max(value = 255, message = "Description must not exceed 255 characters")
        String description
) {
}

package com.banking.service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DepositResponseDTO(
        UUID transactionId,
        String type,
        BigDecimal amountDeposited,
        BigDecimal balanceAfter,
        String description,
        Instant timestamp
) 
{}

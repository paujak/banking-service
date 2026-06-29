package com.banking.service.service.dto;

import com.banking.service.constant.TransactionType;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;

/**
 * Service-layer command object carrying parameters for a financial transaction.
 *
 * @param amount               transaction amount
 * @param description          optional free-text note
 * @param type                 transaction type (DEPOSIT, WITHDRAWAL, EXCHANGE_OUT)
 * @param sourceAccountId      UUID of the account to debit; used for withdrawals and exchanges
 * @param destinationAccountId UUID of the account to credit; used for deposits and exchanges
 */
@Builder
public record TransactionRequestDTO(
        BigDecimal amount,
        String description,
        TransactionType type,
        UUID sourceAccountId,
        UUID destinationAccountId
) {
}

package com.banking.service.service.dto;

import lombok.Builder;

@Builder
public record ExchangeResultDTO(
        TransactionResultDTO debitTransaction,
        TransactionResultDTO creditTransaction
) {
}

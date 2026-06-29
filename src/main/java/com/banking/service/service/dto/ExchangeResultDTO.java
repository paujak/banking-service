package com.banking.service.service.dto;

import lombok.Builder;

/**
 * Holds the pair of transactions produced by a currency exchange operation.
 *
 * @param debitTransaction  EXCHANGE_OUT transaction debited from the source account
 * @param creditTransaction EXCHANGE_IN transaction credited to the destination account
 */
@Builder
public record ExchangeResultDTO(
        TransactionResultDTO debitTransaction,
        TransactionResultDTO creditTransaction
) {
}

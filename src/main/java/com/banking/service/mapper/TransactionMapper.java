package com.banking.service.mapper;

import com.banking.service.controller.dto.DepositResponse;
import com.banking.service.controller.dto.ExchangeResponse;
import com.banking.service.controller.dto.TransactionResponse;
import com.banking.service.controller.dto.WithdrawalResponse;
import com.banking.service.entity.Transaction;
import com.banking.service.service.dto.ExchangeResultDTO;
import com.banking.service.service.dto.TransactionResultDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting {@link Transaction} entities to various response DTOs.
 */
@Mapper(componentModel = "spring")
public interface TransactionMapper {

    /**
     * Maps a {@link Transaction} entity to a {@link TransactionResultDTO}.
     *
     * @param transaction source entity
     * @return mapped service DTO
     */
    TransactionResultDTO toTransactionResultDto(Transaction transaction);

    /**
     * Maps a {@link TransactionResultDTO} to a {@link DepositResponse}.
     * Maps {@code currency.code} → {@code currencyCode} and {@code id} → {@code transactionId}.
     *
     * @param transactionResultDTO source DTO
     * @return deposit response
     */
    @Mapping(source = "currency.code", target = "currencyCode")
    @Mapping(source = "id", target = "transactionId")
    DepositResponse toDepositResponse(TransactionResultDTO transactionResultDTO);

    /**
     * Maps a {@link TransactionResultDTO} to a {@link WithdrawalResponse}.
     * Maps {@code currency.code} → {@code currencyCode} and {@code id} → {@code transactionId}.
     *
     * @param transactionResultDTO source DTO
     * @return withdrawal response
     */
    @Mapping(source = "currency.code", target = "currencyCode")
    @Mapping(source = "id", target = "transactionId")
    WithdrawalResponse toWithdrawalResponse(TransactionResultDTO transactionResultDTO);

    /**
     * Maps a {@link TransactionResultDTO} to a {@link TransactionResponse}.
     * Maps {@code currency.code} → {@code currencyCode} and {@code id} → {@code transactionId}.
     *
     * @param transactionResultDTO source DTO
     * @return transaction history entry response
     */
    @Mapping(source = "currency.code", target = "currencyCode")
    @Mapping(source = "id", target = "transactionId")
    TransactionResponse toTransactionResponse(TransactionResultDTO transactionResultDTO);

    /**
     * Assembles an {@link ExchangeResponse} from an {@link ExchangeResultDTO}.
     * Amounts, currency codes, rate, description, and timestamp are taken from the debit and credit legs.
     *
     * @param exchangeResultDTO contains the debit and credit transaction result DTOs
     * @return currency exchange response
     */
    @Mapping(source = "debitTransaction.amount", target = "amountInSourceCurrency")
    @Mapping(source = "creditTransaction.amount", target = "amountInTargetCurrency")
    @Mapping(source = "debitTransaction.currency.code", target = "sourceCurrencyCode")
    @Mapping(source = "creditTransaction.currency.code", target = "targetCurrencyCode")
    @Mapping(source = "debitTransaction.appliedRate", target = "appliedRate")
    @Mapping(source = "debitTransaction.description", target = "description")
    @Mapping(source = "debitTransaction.timestamp", target = "timestamp")
    ExchangeResponse toExchangeResponse(ExchangeResultDTO exchangeResultDTO);
}

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

@Mapper(componentModel = "spring")
public interface TransactionMapper {
    
    TransactionResultDTO toTransactionResultDto(Transaction transaction);
    
    @Mapping(source = "currency.code", target = "currencyCode")
    @Mapping(source = "id", target = "transactionId")
    DepositResponse toDepositResponse(TransactionResultDTO transactionResultDTO);
    
    @Mapping(source = "currency.code", target = "currencyCode")
    @Mapping(source = "id", target = "transactionId")
    WithdrawalResponse toWithdrawalResponse(TransactionResultDTO transactionResultDTO);

    @Mapping(source = "currency.code", target = "currencyCode")
    @Mapping(source = "id", target = "transactionId")
    TransactionResponse toTransactionResponse(TransactionResultDTO transactionResultDTO);

    @Mapping(source = "debitTransaction.amount", target = "amountInSourceCurrency")
    @Mapping(source = "creditTransaction.amount", target = "amountInTargetCurrency")
    @Mapping(source = "debitTransaction.currency.code", target = "sourceCurrencyCode")
    @Mapping(source = "creditTransaction.currency.code", target = "targetCurrencyCode")
    @Mapping(source = "debitTransaction.appliedRate", target = "appliedRate")
    @Mapping(source = "debitTransaction.description", target = "description")
    @Mapping(source = "debitTransaction.timestamp", target = "timestamp")
    ExchangeResponse toExchangeResponse(ExchangeResultDTO exchangeResultDTO);
}

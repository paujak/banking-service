package com.banking.service.mapper;

import com.banking.service.controller.dto.DepositResponse;
import com.banking.service.controller.dto.WithdrawalResponse;
import com.banking.service.entity.Transaction;
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
}

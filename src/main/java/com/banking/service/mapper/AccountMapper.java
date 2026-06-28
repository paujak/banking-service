package com.banking.service.mapper;

import com.banking.service.controller.dto.AccountResponse;
import com.banking.service.entity.Account;
import com.banking.service.service.dto.AccountDTO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {
    
    @Mapping(source = "currency.code", target = "currencyCode")
    AccountDTO toDto(Account account);
    
    List<AccountDTO> toDtoList(List<Account> accounts);
    
    AccountResponse toResponse(AccountDTO accountDTO);
    
    List<AccountResponse> toResponseList(List<AccountDTO> accountDTOs);
}

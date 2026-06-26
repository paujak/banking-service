package com.banking.service.mapper;

import com.banking.service.dto.AccountResponseDTO;
import com.banking.service.entity.Account;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {
    
    @Mapping(source = "currency.code", target = "currencyCode")
    AccountResponseDTO toDto(Account account);
    
    List<AccountResponseDTO> toDtoList(List<Account> accounts);
}

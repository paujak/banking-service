package com.banking.service.mapper;

import com.banking.service.controller.dto.AccountResponse;
import com.banking.service.entity.Account;
import com.banking.service.service.dto.AccountDTO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between {@link Account} entities,
 * {@link AccountDTO} service DTOs, and {@link AccountResponse} API responses.
 */
@Mapper(componentModel = "spring")
public interface AccountMapper {

    /**
     * Maps an {@link Account} entity to an {@link AccountDTO}.
     * The {@code currency.code} field is mapped to {@code currencyCode}.
     *
     * @param account source entity
     * @return mapped DTO
     */
    @Mapping(source = "currency.code", target = "currencyCode")
    AccountDTO toDto(Account account);

    /**
     * Maps a list of {@link Account} entities to a list of {@link AccountDTO}s.
     *
     * @param accounts source entities
     * @return list of mapped DTOs
     */
    List<AccountDTO> toDtoList(List<Account> accounts);

    /**
     * Maps an {@link AccountDTO} to an {@link AccountResponse}.
     *
     * @param accountDTO source DTO
     * @return API response object
     */
    AccountResponse toResponse(AccountDTO accountDTO);

    /**
     * Maps a list of {@link AccountDTO}s to a list of {@link AccountResponse}s.
     *
     * @param accountDTOs source DTOs
     * @return list of API response objects
     */
    List<AccountResponse> toResponseList(List<AccountDTO> accountDTOs);
}

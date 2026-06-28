package com.banking.service.service.dto;

public record CurrencyDTO(
        String code,
        String name,
        Boolean isActive
) {
}

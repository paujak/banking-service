package com.banking.service.service.dto;

/**
 * Lightweight representation of a currency for use in service and controller DTOs.
 *
 * @param code ISO 4217 three-letter currency code (e.g. {@code "EUR"})
 * @param name human-readable currency name (e.g. {@code "Euro"})
 */
public record CurrencyDTO(
        String code,
        String name
) {
}

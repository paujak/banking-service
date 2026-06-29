package com.banking.service.service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;

/**
 * Payload sent to the external debit-check service when a withdrawal is initiated.
 * {@code null} fields are excluded from serialisation.
 *
 * @param amount    the withdrawal amount
 * @param fullName  full name of the account holder
 * @param accountId UUID of the account being debited
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DebitStatusRequestDTO(
        BigDecimal amount,
        String fullName,
        UUID accountId
) {
}

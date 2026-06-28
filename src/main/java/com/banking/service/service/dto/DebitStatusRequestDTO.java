package com.banking.service.service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DebitStatusRequestDTO(
        BigDecimal amount,
        String fullName,
        UUID accountId
) {
}

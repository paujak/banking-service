package com.banking.service.service;

import com.banking.service.exception.AccountNotFoundException;
import com.banking.service.exception.ExternalServiceException;
import com.banking.service.repository.AccountRepository;
import com.banking.service.service.dto.DebitStatusRequestDTO;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Sends withdrawal notifications to a configured external debit-check service via HTTP POST.
 */
@Service
public class ExternalLoggingService {

    private final AccountRepository accountRepository;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${banking-service.account.withdrawal.external-service.url}")
    private String debitCheckUrl;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public ExternalLoggingService(AccountRepository accountRepository,
                                  CloseableHttpClient httpClient,
                                  ObjectMapper objectMapper) {
        this.accountRepository = accountRepository;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Notifies the external debit-check service about a withdrawal operation.
     *
     * @param accountId UUID of the account being debited
     * @param amount    the amount being withdrawn
     * @throws AccountNotFoundException if no account matches the given ID
     * @throws ExternalServiceException if the external service returns a non-200 response or is unreachable
     */
    public void notifyWithdrawal(UUID accountId, BigDecimal amount) {
        var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        BasicClassicHttpRequest request = new BasicClassicHttpRequest(Method.POST, debitCheckUrl);
        DebitStatusRequestDTO payload = DebitStatusRequestDTO.builder()
                .fullName(account.getUser().getFullName())
                .amount(amount)
                .accountId(account.getId())
                .build();
        String requestBody = objectMapper.writeValueAsString(payload);
        request.setEntity(HttpEntities.create(requestBody, ContentType.APPLICATION_JSON));

        try {
            httpClient.execute(request, response -> {
                int statusCode = response.getCode();
                if (statusCode != 200) {
                    logger.error("External service call failed with status code: {}", statusCode);
                    throw new ExternalServiceException("External service call failed with status code: " + statusCode);
                }
                return null;
            });
        } catch (IOException e) {
            logger.error("Error occurred while calling external service", e);
            throw new ExternalServiceException("External service unavailable");
        }
    }
}

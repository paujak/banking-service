package com.banking.service.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.retry.ExhaustedRetryException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;


/**
 * Global exception handler returning RFC 7807 Problem Details for all error responses.
 * Internal stack traces are never exposed in the {@code detail} field.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles bean validation errors (e.g. {@code @NotNull}, {@code @DecimalMin}).
     *
     * @param ex      the validation exception
     * @param request the current HTTP request
     * @return 400 Bad Request with problem details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request validation failed");
        pd.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    /**
     * Handles requests to unknown static resource paths.
     *
     * @param ex      the not-found exception
     * @param request the current HTTP request
     * @return 404 Not Found with problem details
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            NoResourceFoundException ex,
            HttpServletRequest request) {

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, "Resource not found");
        pd.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }
    
    /**
     * Handles requests for a user that does not exist.
     *
     * @param ex      the not-found exception
     * @param request the current HTTP request
     * @return 404 Not Found with problem details
     */
    @ExceptionHandler(UserIsNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            UserIsNotFoundException ex,
            HttpServletRequest request
    ) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, "User not found"
        );
        pd.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    /**
     * Handles withdrawal or exchange attempts where the account balance is insufficient.
     *
     * @param ex      the insufficient funds exception
     * @param request the current HTTP request
     * @return 400 Bad Request with problem details
     */
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ProblemDetail> handleInsufficientFunds(
            InsufficientFundsException ex,
            HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Insufficient funds for the transaction");
        pd.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    /**
     * Handles requests with a malformed or unreadable request body.
     *
     * @param ex      the message-not-readable exception
     * @param request the current HTTP request
     * @return 400 Bad Request with problem details
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request is invalid or malformed");
        pd.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    /**
     * Handles transactions referencing an account that does not exist.
     *
     * @param ex      the account not-found exception
     * @param request the current HTTP request
     * @return 422 Unprocessable Content with problem details
     */
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleAccountNotFound(
            AccountNotFoundException ex,
            HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_CONTENT, "Transaction cannot be completed");
        pd.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    /**
     * Handles concurrent account modification detected after retries are exhausted.
     *
     * @param ex      the concurrent modification exception
     * @param request the current HTTP request
     * @return 409 Conflict with problem details
     */
    @ExceptionHandler(AccountConcurrentModificationException.class)
    public ResponseEntity<ProblemDetail> handleConcurrentModification(
            AccountConcurrentModificationException ex,
            HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, ex.getMessage());
        pd.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    /**
     * Handles currency exchange requests where source and destination are the same account.
     *
     * @param ex      the same-account exchange exception
     * @param request the current HTTP request
     * @return 400 Bad Request with problem details
     */
    @ExceptionHandler(CurrencyExchangeWithinSameAccountException.class)
    public ResponseEntity<ProblemDetail> handleSameAccount(
            CurrencyExchangeWithinSameAccountException ex,
            HttpServletRequest request
    ) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Currency exchange cannot be performed within the same account");
        pd.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }
    
    /**
     * Handles failures from the external debit-check service.
     *
     * @param ex      the external service exception
     * @param request the current HTTP request
     * @return 500 Internal Server Error with problem details
     */
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ProblemDetail> handleExternalServiceException(
            ExternalServiceException ex,
            HttpServletRequest request
    ) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "External service error"
        );
        pd.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    /**
     * Handles currency exchange requests for which no rate is configured.
     *
     * @param ex      the no-rate exception
     * @param request the current HTTP request
     * @return 500 Internal Server Error with problem details
     */
    @ExceptionHandler(NoExchangeRateDefined.class)
    public ResponseEntity<ProblemDetail> handleNoExchangeRateDefined(
            NoExchangeRateDefined ex,
            HttpServletRequest request
    ) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "No exchange rate defined for the requested currency pair"
        );
        pd.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }

    /**
     * Handles Spring Retry's wrapper exception when all retries are exhausted.
     * Unwraps the cause and re-delegates to the appropriate specific handler.
     *
     * @param ex      the exhausted-retry exception wrapping the original cause
     * @param request the current HTTP request
     * @return response produced by the specific handler for the unwrapped cause
     */
    @ExceptionHandler(ExhaustedRetryException.class)
    public ResponseEntity<ProblemDetail> handleExhaustedRetry(
            ExhaustedRetryException ex,
            HttpServletRequest request) {
        // Spring Retry wraps business exceptions in ExhaustedRetryException when no @Recover matches.
        // Unwrap the cause and re-delegate to the appropriate specific handler.
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        if (cause instanceof AccountNotFoundException accountEx) {
            return handleAccountNotFound(accountEx, request);
        }
        if (cause instanceof InsufficientFundsException fundsEx) {
            return handleInsufficientFunds(fundsEx, request);
        }
        if (cause instanceof CurrencyExchangeWithinSameAccountException sameAccountEx) {
            return handleSameAccount(sameAccountEx, request);
        }
        return handleAll(cause instanceof Exception e ? e : ex, request);
    }

    /**
     * Catch-all handler for any unhandled exception.
     *
     * @param ex      the unhandled exception
     * @param request the current HTTP request
     * @return 500 Internal Server Error with generic problem details
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleAll(
            Exception ex,
            HttpServletRequest request) {

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        pd.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }
}

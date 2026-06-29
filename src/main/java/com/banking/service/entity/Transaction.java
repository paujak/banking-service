package com.banking.service.entity;

import com.banking.service.constant.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.persistence.PrePersist;

/**
 * JPA entity recording a single financial movement on an {@link Account}.
 * All fields are immutable after creation. The {@code timestamp} is set via
 * a {@link jakarta.persistence.PrePersist} hook if not supplied at construction time.
 */
@Entity
@Table(name = "transaction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    private UUID id;

    @Column(name = "correlation_id", updatable = false)
    private UUID correlationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private Account account;

    @Column(name = "type", nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(name = "amount", nullable = false, updatable = false)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "currency_code", nullable = false, updatable = false)
    private Currency currency;

    @Column(name = "applied_rate", updatable = false)
    private BigDecimal appliedRate;

    @Column(name = "balance_after", nullable = false, updatable = false)
    private BigDecimal balanceAfter;

    @Column(name = "description", length = 255, updatable = false)
    private String description;

    @Column(name = "timestamp", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private Instant timestamp;

    @PrePersist
    protected void prePersist() {
        if (this.timestamp == null) {
            this.timestamp = Instant.now();
        }
    }

    /**
     * Creates a new Transaction.
     *
     * @param account       the account this transaction belongs to
     * @param correlationId shared UUID linking the two legs of a currency exchange; {@code null} otherwise
     * @param type          transaction type (DEPOSIT, WITHDRAWAL, EXCHANGE_IN, EXCHANGE_OUT)
     * @param amount        transaction amount
     * @param balanceAfter  account balance immediately after this transaction
     * @param currency      currency of the transaction
     * @param appliedRate   exchange rate applied; {@code null} for non-exchange transactions
     * @param description   optional free-text note
     * @param timestamp     time of the transaction; set automatically on persist if {@code null}
     */
    @Builder
    public Transaction(Account account,
                       UUID correlationId,
                       TransactionType type,
                       BigDecimal amount,
                       BigDecimal balanceAfter,
                       Currency currency,
                       BigDecimal appliedRate,
                       String description,
                       Instant timestamp) {
        this.account = account;
        this.correlationId = correlationId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.appliedRate = appliedRate;
        this.description = description;
        this.balanceAfter = balanceAfter;
        this.timestamp = timestamp;
    }
}


package com.banking.service.entity;

import com.banking.service.constant.TransactionType;
import com.banking.service.exception.InsufficientFundsException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "transaction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private Account account;
    
    @Column(name = "type", nullable = false, updatable = false)
    private TransactionType type;
    
    @Column(name = "amount", nullable = false, updatable = false)
    private BigDecimal amount;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "currency_code", nullable = false, updatable = false)
    private Currency currency;
    
    @Column(name = "applied_rate", nullable = false, updatable = false)
    private BigDecimal appliedRate;
    
    @Column(name = "balance_after", nullable = false, updatable = false)
    private BigDecimal balanceAfter;
    
    @Column(name = "timestamp", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    @CreationTimestamp
    private Instant timestamp;
    
    @Builder
    public Transaction(Account account, TransactionType type, BigDecimal amount, Currency currency, BigDecimal appliedRate) throws InsufficientFundsException {
        this.account = account;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.appliedRate = appliedRate != null ? appliedRate : BigDecimal.ONE;
        BigDecimal currentBalance = account.getBalance();
        if (TransactionType.CREDIT.equals(type) || TransactionType.EXCHANGE_IN.equals(type)) {
            this.balanceAfter = currentBalance.add(amount);
        } else if (TransactionType.DEBIT.equals(type) || TransactionType.EXCHANGE_OUT.equals(type)) {
            if (currentBalance.compareTo(amount) < 0) {
                throw new InsufficientFundsException("Insufficient balance for debit transaction");
            }
            this.balanceAfter = currentBalance.subtract(amount);
        } else {
            throw new IllegalArgumentException("Invalid transaction type: " + type);
        }
        this.account.setBalance(this.balanceAfter);
    }
}

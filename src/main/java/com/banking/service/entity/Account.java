package com.banking.service.entity;

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
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
    
    @Column(name = "account_number", unique = true, nullable = false, updatable = false)
    private String accountNumber;
    
    @Setter
    @Column(name = "account_name")
    private String accountName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;
    
    @Setter
    @Column(name = "balance", nullable = false)
    private BigDecimal balance;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "currency_code", nullable = false, updatable = false)
    private Currency currency;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    @CreationTimestamp
    private Instant createdAt;
    
    @Builder
    public Account(String accountNumber, String accountName, User user, BigDecimal initialBalance, Currency currency) {
        this.accountNumber = accountNumber;
        this.accountName = accountName == null || accountName.isBlank() ? "Account " + accountNumber : accountName;
        this.user = user;
        this.balance = initialBalance;
        this.currency = currency;
    }
}

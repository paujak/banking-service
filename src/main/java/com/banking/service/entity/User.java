package com.banking.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    private UUID id;
    
    @Setter
    @Column(name = "username", unique = true, nullable = false, updatable = false)
    private String username;
    
    @Setter
    @Column(name = "full_name", nullable = false)
    private String fullName;
    
    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER)
    private List<Account> accounts;
    
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    @CreationTimestamp
    private Instant createdAt;
    
    @Builder
    public User(String username, String fullName) {
        this.username = username;
        this.fullName = fullName;
    }
}

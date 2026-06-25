package com.banking.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "currency")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Currency {
    
    @Id
    @Column(name = "code", nullable = false, updatable = false, length = 3)
    private String code;
    
    @Setter
    @Column(name = "name", nullable = false)
    private String name;
    
    @Setter
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
    
    @Builder
    public Currency(String code, String name, Boolean isActive) {
        this.code = code;
        this.name = name;
        this.isActive = isActive;
    }
}

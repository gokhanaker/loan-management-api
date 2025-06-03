package com.applab.loan_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String surname;
    
    @Column(name = "credit_limit", nullable = false)
    private BigDecimal creditLimit;
    
    @Column(name = "used_credit_limit", nullable = false)
    private BigDecimal usedCreditLimit;
    
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<Loan> loans = new ArrayList<>();
} 
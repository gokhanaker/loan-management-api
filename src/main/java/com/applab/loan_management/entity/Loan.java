package com.applab.loan_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loans")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Loan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    @Column(name = "loan_amount", nullable = false)
    private BigDecimal loanAmount;
    
    @Column(name = "number_of_installments", nullable = false)
    private Integer numberOfInstallments;
    
    @Column(name = "interest_rate", nullable = false)
    private BigDecimal interestRate;
    
    @Column(name = "create_date", nullable = false)
    private LocalDateTime createDate;
    
    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid;
    
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL)
    private List<LoanInstallment> installments = new ArrayList<>();
} 
package com.applab.loan_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loan_installments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanInstallment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    @Column(name = "paid_amount")
    private BigDecimal paidAmount;
    
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    
    @Column(name = "payment_date")
    private LocalDate paymentDate;
    
    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid;
} 
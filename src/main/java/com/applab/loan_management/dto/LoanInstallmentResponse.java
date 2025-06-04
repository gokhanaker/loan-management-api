package com.applab.loan_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanInstallmentResponse {
    private Long id;
    private Long loanId;
    private BigDecimal amount;
    private LocalDate dueDate;
    private LocalDate paymentDate;
    private BigDecimal paidAmount;
    private Boolean isPaid;
    private Integer installmentNumber;
} 
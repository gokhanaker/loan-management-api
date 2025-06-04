package com.applab.loan_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanListResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private String customerSurname;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private Integer numberOfInstallments;
    private LocalDateTime createDate;
    private Boolean isPaid;
    private BigDecimal totalAmount; // amount + interest
    private Integer remainingInstallments;
} 
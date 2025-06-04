package com.applab.loan_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayLoanResponse {
    private int installmentsPaid;
    private BigDecimal totalAmountSpent;
    private boolean isLoanFullyPaid;
    private String message;
} 
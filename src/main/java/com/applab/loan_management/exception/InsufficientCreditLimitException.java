package com.applab.loan_management.exception;

import java.math.BigDecimal;

public class InsufficientCreditLimitException extends RuntimeException {
    public InsufficientCreditLimitException(BigDecimal availableCredit, BigDecimal requiredAmount) {
        super(String.format("Insufficient credit limit. Available: %.2f, Required: %.2f", 
              availableCredit, requiredAmount));
    }
} 
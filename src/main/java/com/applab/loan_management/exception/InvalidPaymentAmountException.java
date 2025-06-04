package com.applab.loan_management.exception;

import java.math.BigDecimal;

public class InvalidPaymentAmountException extends RuntimeException {
    public InvalidPaymentAmountException(BigDecimal amount, BigDecimal minimumAmount) {
        super(String.format("Payment amount %.2f is insufficient. Minimum amount required to pay at least one installment: %.2f", 
              amount, minimumAmount));
    }
    
    public InvalidPaymentAmountException(String message) {
        super(message);
    }
} 
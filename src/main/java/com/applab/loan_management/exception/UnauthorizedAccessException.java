package com.applab.loan_management.exception;

public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException(String message) {
        super(message);
    }
    
    public UnauthorizedAccessException(Long customerId, Long loanId) {
        super(String.format("Customer %d is not authorized to access loan %d", customerId, loanId));
    }
} 
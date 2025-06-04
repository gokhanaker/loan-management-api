package com.applab.loan_management.exception;

public class LoanDataAccessException extends RuntimeException {
    public LoanDataAccessException(String message) {
        super(message);
    }
    
    public LoanDataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
} 
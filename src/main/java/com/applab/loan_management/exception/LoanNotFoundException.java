package com.applab.loan_management.exception;

public class LoanNotFoundException extends RuntimeException {
    public LoanNotFoundException(Long loanId) {
        super("Loan not found with ID: " + loanId);
    }
} 
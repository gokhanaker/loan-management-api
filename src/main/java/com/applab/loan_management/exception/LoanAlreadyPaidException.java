package com.applab.loan_management.exception;

public class LoanAlreadyPaidException extends RuntimeException {
    public LoanAlreadyPaidException(Long loanId) {
        super("Loan with ID " + loanId + " is already fully paid");
    }
} 
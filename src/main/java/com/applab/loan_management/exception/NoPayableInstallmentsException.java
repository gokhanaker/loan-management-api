package com.applab.loan_management.exception;

public class NoPayableInstallmentsException extends RuntimeException {
    public NoPayableInstallmentsException(Long loanId) {
        super("No installments available for payment within the next 3 months for loan ID: " + loanId);
    }
} 
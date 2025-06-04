package com.applab.loan_management.exception;

public class AdminCannotCreateLoanException extends RuntimeException {
    public AdminCannotCreateLoanException() {
        super("Admin users cannot create loans. Only customers with credit limits can create loans.");
    }
} 
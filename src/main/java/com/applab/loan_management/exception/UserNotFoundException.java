package com.applab.loan_management.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String email) {
        super("User not found with email: " + email);
    }
} 
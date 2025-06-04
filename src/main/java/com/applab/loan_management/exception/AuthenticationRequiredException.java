package com.applab.loan_management.exception;

public class AuthenticationRequiredException extends RuntimeException {
    public AuthenticationRequiredException() {
        super("Authentication required. Please provide a valid JWT token in the Authorization header.");
    }
    
    public AuthenticationRequiredException(String message) {
        super(message);
    }
} 
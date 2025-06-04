package com.applab.loan_management.exception;

public class CustomerAccessDeniedException extends RuntimeException {
    public CustomerAccessDeniedException(Long requestedCustomerId, Long currentCustomerId) {
        super(String.format("Access denied. Customer %d cannot access data for customer %d", 
              currentCustomerId, requestedCustomerId));
    }
    
    public CustomerAccessDeniedException(String message) {
        super(message);
    }
} 
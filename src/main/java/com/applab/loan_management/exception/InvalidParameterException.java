package com.applab.loan_management.exception;

public class InvalidParameterException extends RuntimeException {
    public InvalidParameterException(String parameterName, String reason) {
        super(String.format("Invalid parameter '%s': %s", parameterName, reason));
    }
} 
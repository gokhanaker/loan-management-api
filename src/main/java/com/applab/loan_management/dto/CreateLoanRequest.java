package com.applab.loan_management.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLoanRequest {
    
    @NotNull(message = "Customer ID is required")
    private Long customerId;
    
    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "100", message = "Loan amount must be at least 100")
    private BigDecimal amount;
    
    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.1", message = "Interest rate must be at least 0.1")
    @DecimalMax(value = "0.5", message = "Interest rate must be at most 0.5")
    private BigDecimal interestRate;
    
    @NotNull(message = "Number of installments is required")
    private Integer numberOfInstallments;
    
    // Custom validation method
    @AssertTrue(message = "Number of installments must be 6, 9, 12, or 24")
    public boolean isValidNumberOfInstallments() {
        if (numberOfInstallments == null) {
            return true; // Let @NotNull handle null case
        }
        return numberOfInstallments.equals(6) || numberOfInstallments.equals(9) || 
               numberOfInstallments.equals(12) || numberOfInstallments.equals(24);
    }
} 
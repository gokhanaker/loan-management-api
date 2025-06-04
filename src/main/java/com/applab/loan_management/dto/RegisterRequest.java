package com.applab.loan_management.dto;

import com.applab.loan_management.constants.Role;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;

    @NotNull(message = "Role is required")
    private Role role;
    
    @NotBlank(message = "Name is required")
    private String name;
    
    @NotBlank(message = "Surname is required")
    private String surname;
    
    // Credit fields - only for CUSTOMER role, null for ADMIN
    @DecimalMin(value = "0.0", inclusive = true, message = "Credit limit must be positive")
    private BigDecimal creditLimit;
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Used credit limit must be positive")
    private BigDecimal usedCreditLimit;

    // Custom validation to ensure credit fields are provided for CUSTOMER role
    @AssertTrue(message = "Credit limit is required for CUSTOMER role")
    public boolean isCreditLimitValidForRole() {
        if (role == Role.CUSTOMER) {
            return creditLimit != null;
        }
        return true; // For ADMIN, credit limit can be null
    }

    @AssertTrue(message = "Used credit limit is required for CUSTOMER role")
    public boolean isUsedCreditLimitValidForRole() {
        if (role == Role.CUSTOMER) {
            return usedCreditLimit != null;
        }
        return true; // For ADMIN, used credit limit can be null
    }
} 
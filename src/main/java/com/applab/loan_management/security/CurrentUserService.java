package com.applab.loan_management.security;

import com.applab.loan_management.constants.Role;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserService {

    private final JwtUtil jwtUtil;

    /**
     * Get the current authenticated user's customer ID from JWT token
     */
    public Long getCurrentCustomerId() {
        String jwt = getCurrentJwtToken();
        if (jwt == null) {
            return null;
        }
        return jwtUtil.extractCustomerId(jwt);
    }

    /**
     * Get the current authenticated user's role from JWT token
     */
    public Role getCurrentUserRole() {
        String jwt = getCurrentJwtToken();
        if (jwt == null) {
            return null;
        }
        return jwtUtil.extractRole(jwt);
    }

    /**
     * Check if the current user is an admin
     */
    public boolean isCurrentUserAdmin() {
        Role role = getCurrentUserRole();
        return role != null && role == Role.ADMIN;
    }

    /**
     * Check if the current user can access the specified customer's data
     * Admins can access any customer's data, regular customers can only access their own
     */
    public boolean canAccessCustomerData(Long customerId) {
        if (isCurrentUserAdmin()) {
            return true; // Admins can access any customer's data
        }
        
        Long currentCustomerId = getCurrentCustomerId();
        return currentCustomerId != null && currentCustomerId.equals(customerId);
    }

    /**
     * Extract JWT token from the current security context
     */
    private String getCurrentJwtToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getCredentials() == null) {
            return null;
        }
        
        // The JWT token should be available in the credentials
        Object credentials = authentication.getCredentials();
        if (credentials instanceof String) {
            return (String) credentials;
        }
        
        return null;
    }
} 
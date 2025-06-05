package com.applab.loan_management.util;

import com.applab.loan_management.constants.Role;
import com.applab.loan_management.dto.AuthenticationResponse;
import com.applab.loan_management.dto.RegisterRequest;
import com.applab.loan_management.dto.RegisterResponse;
import com.applab.loan_management.entity.Customer;
import com.applab.loan_management.security.JwtUtil;
import com.applab.loan_management.service.CustomerUserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for authentication-related operations including DTO conversions and JWT token generation.
 * This class centralizes all authentication mapping logic to improve code maintainability and reusability.
 */
public final class AuthMapperUtil {

    private AuthMapperUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Creates a Customer entity from RegisterRequest
     *
     * @param request the registration request
     * @param passwordEncoder the password encoder service
     * @return Customer entity ready to be saved
     */
    public static Customer createCustomerFromRequest(RegisterRequest request, PasswordEncoder passwordEncoder) {
        if (request.getRole() == Role.CUSTOMER) {
            // Create customer with credit fields
            return Customer.builder()
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(request.getRole())
                    .name(request.getName())
                    .surname(request.getSurname())
                    .creditLimit(request.getCreditLimit())
                    .usedCreditLimit(request.getUsedCreditLimit())
                    .build();
        } else {
            // Create admin without credit fields
            return Customer.builder()
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(request.getRole())
                    .name(request.getName())
                    .surname(request.getSurname())
                    .creditLimit(null)
                    .usedCreditLimit(null)
                    .build();
        }
    }

    /**
     * Converts a Customer entity to RegisterResponse DTO
     *
     * @param customer the customer entity to convert
     * @param jwtToken the JWT token to include in the response
     * @return RegisterResponse DTO
     */
    public static RegisterResponse toRegisterResponse(Customer customer, String jwtToken) {
        return RegisterResponse.builder()
                .token(jwtToken)
                .message(generateRegistrationMessage(customer))
                .userId(customer.getId())
                .user(RegisterResponse.UserProfile.builder()
                        .name(customer.getName())
                        .surname(customer.getSurname())
                        .email(customer.getEmail())
                        .role(customer.getRole().name())
                        .build())
                .registrationTime(LocalDateTime.now())
                .build();
    }

    /**
     * Converts a Customer entity to AuthenticationResponse DTO
     *
     * @param customer the customer entity to convert
     * @param jwtToken the JWT token to include in the response
     * @return AuthenticationResponse DTO
     */
    public static AuthenticationResponse toAuthenticationResponse(Customer customer, String jwtToken) {
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .message(generateAuthenticationMessage(customer))
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Generates a JWT token with customer claims
     *
     * @param customer the customer entity to create token for
     * @param jwtUtil the JWT utility service
     * @return JWT token string
     */
    public static String generateJwtToken(Customer customer, JwtUtil jwtUtil) {
        Map<String, Object> extraClaims = createCustomerClaims(customer);
        return jwtUtil.generateToken(extraClaims, new CustomerUserDetails(customer));
    }

    /**
     * Creates customer claims map for JWT token
     *
     * @param customer the customer entity
     * @return map of claims to include in JWT token
     */
    public static Map<String, Object> createCustomerClaims(Customer customer) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", customer.getRole().name());
        claims.put("userId", customer.getId());
        claims.put("customerId", customer.getId());
        return claims;
    }

    /**
     * Generates a welcome message for registration
     *
     * @param customer the customer entity
     * @return formatted welcome message
     */
    public static String generateRegistrationMessage(Customer customer) {
        return String.format("User account created successfully with ID: %d", customer.getId());
    }

    /**
     * Generates a welcome message for authentication
     *
     * @param customer the customer entity
     * @return formatted welcome message
     */
    public static String generateAuthenticationMessage(Customer customer) {
        return String.format("Successful login, %s!", customer.getId());
    }
} 
package com.applab.loan_management.service;

import com.applab.loan_management.constants.Role;
import com.applab.loan_management.dto.AuthenticationRequest;
import com.applab.loan_management.dto.AuthenticationResponse;
import com.applab.loan_management.dto.RegisterRequest;
import com.applab.loan_management.dto.RegisterResponse;
import com.applab.loan_management.entity.Customer;
import com.applab.loan_management.exception.EmailAlreadyExistsException;
import com.applab.loan_management.exception.InvalidCredentialsException;
import com.applab.loan_management.exception.UserNotFoundException;
import com.applab.loan_management.repository.CustomerRepository;
import com.applab.loan_management.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public RegisterResponse register(RegisterRequest request) {
        // Check if email already exists
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        Customer customer;

        if (request.getRole() == Role.CUSTOMER) {
            // Create customer with credit fields
            customer = Customer.builder()
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
            customer = Customer.builder()
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(request.getRole())
                    .name(request.getName())
                    .surname(request.getSurname())
                    .creditLimit(null)
                    .usedCreditLimit(null)
                    .build();
        }

        customerRepository.save(customer);

        // Generate JWT token using helper method
        String jwtToken = generateJwtToken(customer);
        
        return RegisterResponse.builder()
                .token(jwtToken)
                .message(String.format("User account created successfully with ID: %d", customer.getId()))
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

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        // Find customer by email
        Customer customer = customerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException(request.getEmail()));

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), customer.getPassword())) {
            throw new InvalidCredentialsException();
        }

        // Generate JWT token using helper method
        String jwtToken = generateJwtToken(customer);
        
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .message(String.format("Successful login, %s!", customer.getName()))
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Helper method to generate JWT token with customer claims
     * Reduces code duplication between register and authenticate methods
     */
    private String generateJwtToken(Customer customer) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", customer.getRole().name());
        extraClaims.put("userId", customer.getId());
        extraClaims.put("customerId", customer.getId());

        return jwtUtil.generateToken(extraClaims, new CustomerUserDetails(customer));
    }
} 
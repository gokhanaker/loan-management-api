package com.applab.loan_management.service;

import com.applab.loan_management.dto.AuthenticationRequest;
import com.applab.loan_management.dto.AuthenticationResponse;
import com.applab.loan_management.dto.RegisterRequest;
import com.applab.loan_management.entity.Customer;
import com.applab.loan_management.repository.CustomerRepository;
import com.applab.loan_management.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthenticationResponse register(RegisterRequest request) {
        // Check if email already exists
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Create customer (which can be CUSTOMER or ADMIN role)
        var customer = Customer.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .name(request.getName())
                .surname(request.getSurname())
                .creditLimit(request.getCreditLimit() != null ? request.getCreditLimit() : BigDecimal.ZERO)
                .usedCreditLimit(request.getUsedCreditLimit() != null ? request.getUsedCreditLimit() : BigDecimal.ZERO)
                .build();

        customerRepository.save(customer);

        // Generate JWT token with additional claims
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", customer.getRole().name());
        extraClaims.put("userId", customer.getId());
        extraClaims.put("customerId", customer.getId());

        var jwtToken = jwtUtil.generateToken(extraClaims, new CustomerUserDetails(customer));
        
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        var customer = customerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), customer.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        // Generate JWT token with additional claims
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", customer.getRole().name());
        extraClaims.put("userId", customer.getId());
        extraClaims.put("customerId", customer.getId());

        var jwtToken = jwtUtil.generateToken(extraClaims, new CustomerUserDetails(customer));
        
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }
} 
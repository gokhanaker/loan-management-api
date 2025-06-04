package com.applab.loan_management.service;

import com.applab.loan_management.constants.Role;
import com.applab.loan_management.dto.AuthenticationRequest;
import com.applab.loan_management.dto.AuthenticationResponse;
import com.applab.loan_management.dto.RegisterRequest;
import com.applab.loan_management.entity.Customer;
import com.applab.loan_management.entity.User;
import com.applab.loan_management.repository.CustomerRepository;
import com.applab.loan_management.repository.UserRepository;
import com.applab.loan_management.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        Customer customer = null;
        if (request.getRole() == Role.CUSTOMER) {
            customer = Customer.builder()
                    .name(request.getName())
                    .surname(request.getSurname())
                    .creditLimit(request.getCreditLimit())
                    .usedCreditLimit(request.getUsedCreditLimit())
                    .build();
            customer = customerRepository.save(customer);
        }

        var user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .customer(customer)
                .build();

        userRepository.save(user);
        
        // Generate JWT token with user details
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole().name());
        extraClaims.put("userId", user.getId());
        if (customer != null) {
            extraClaims.put("customerId", customer.getId());
        }
        
        var jwtToken = jwtUtil.generateToken(extraClaims, user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        // Simple password check
        if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // Generate JWT token with user details
            Map<String, Object> extraClaims = new HashMap<>();
            extraClaims.put("role", user.getRole().name());
            extraClaims.put("userId", user.getId());
            if (user.getCustomer() != null) {
                extraClaims.put("customerId", user.getCustomer().getId());
            }
            
            var jwtToken = jwtUtil.generateToken(extraClaims, user);
            return AuthenticationResponse.builder()
                    .token(jwtToken)
                    .build();
        } else {
            throw new RuntimeException("Invalid credentials");
        }
    }
} 
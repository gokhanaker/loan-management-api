package com.applab.loan_management.service;

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
import com.applab.loan_management.util.AuthMapperUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public RegisterResponse register(RegisterRequest request) {
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        Customer customer = AuthMapperUtil.createCustomerFromRequest(request, passwordEncoder);
        customerRepository.save(customer);

        // Generate JWT token and create response
        String jwtToken = AuthMapperUtil.generateJwtToken(customer, jwtUtil);
        return AuthMapperUtil.toRegisterResponse(customer, jwtToken);
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        Customer customer = customerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException(request.getEmail()));

        // Verify password matches
        if (!passwordEncoder.matches(request.getPassword(), customer.getPassword())) {
            throw new InvalidCredentialsException();
        }

        // Generate JWT token and create response
        String jwtToken = AuthMapperUtil.generateJwtToken(customer, jwtUtil);
        return AuthMapperUtil.toAuthenticationResponse(customer, jwtToken);
    }
} 
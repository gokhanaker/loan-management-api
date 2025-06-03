package com.applab.loan_management.security;

import com.applab.loan_management.dto.AuthenticationRequest;
import com.applab.loan_management.dto.AuthenticationResponse;
import com.applab.loan_management.dto.RegisterRequest;
import com.applab.loan_management.entity.Customer;
import com.applab.loan_management.entity.User;
import com.applab.loan_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        var user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        if (request.getRole() == Role.CUSTOMER) {
            var customer = Customer.builder()
                    .name(request.getName())
                    .surname(request.getSurname())
                    .creditLimit(request.getCreditLimit())
                    .usedCreditLimit(request.getUsedCreditLimit())
                    .build();
            user.setCustomer(customer);
        }

        userRepository.save(user);
        var jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();
        var jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }
} 
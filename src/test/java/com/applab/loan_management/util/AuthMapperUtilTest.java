package com.applab.loan_management.util;

import com.applab.loan_management.constants.Role;
import com.applab.loan_management.dto.AuthenticationResponse;
import com.applab.loan_management.dto.RegisterRequest;
import com.applab.loan_management.dto.RegisterResponse;
import com.applab.loan_management.entity.Customer;
import com.applab.loan_management.security.JwtUtil;
import com.applab.loan_management.security.CustomerUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthMapperUtil Tests")
class AuthMapperUtilTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    private RegisterRequest customerRegisterRequest;
    private RegisterRequest adminRegisterRequest;
    private Customer testCustomer;
    private Customer testAdmin;

    @BeforeEach
    void setUp() {
        customerRegisterRequest = RegisterRequest.builder()
                .email("customer@test.com")
                .password("password123")
                .role(Role.CUSTOMER)
                .name("John")
                .surname("Doe")
                .creditLimit(new BigDecimal("10000.00"))
                .usedCreditLimit(new BigDecimal("0.00"))
                .build();

        adminRegisterRequest = RegisterRequest.builder()
                .email("admin@test.com")
                .password("password123")
                .role(Role.ADMIN)
                .name("Admin")
                .surname("User")
                .build();

        testCustomer = Customer.builder()
                .id(1L)
                .email("customer@test.com")
                .password("encodedPassword")
                .role(Role.CUSTOMER)
                .name("John")
                .surname("Doe")
                .creditLimit(new BigDecimal("10000.00"))
                .usedCreditLimit(new BigDecimal("0.00"))
                .build();

        testAdmin = Customer.builder()
                .id(2L)
                .email("admin@test.com")
                .password("encodedPassword")
                .role(Role.ADMIN)
                .name("Admin")
                .surname("User")
                .creditLimit(null)
                .usedCreditLimit(null)
                .build();
    }

    @Test
    @DisplayName("Should create customer entity from customer register request")
    void shouldCreateCustomerEntityFromCustomerRegisterRequest() {
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        Customer result = AuthMapperUtil.createCustomerFromRequest(customerRegisterRequest, passwordEncoder);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("customer@test.com");
        assertThat(result.getPassword()).isEqualTo("encodedPassword");
        assertThat(result.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(result.getName()).isEqualTo("John");
        assertThat(result.getSurname()).isEqualTo("Doe");
        assertThat(result.getCreditLimit()).isEqualTo(new BigDecimal("10000.00"));
        assertThat(result.getUsedCreditLimit()).isEqualTo(new BigDecimal("0.00"));

        verify(passwordEncoder).encode("password123");
    }

    @Test
    @DisplayName("Should create admin entity from admin register request")
    void shouldCreateAdminEntityFromAdminRegisterRequest() {
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

        Customer result = AuthMapperUtil.createCustomerFromRequest(adminRegisterRequest, passwordEncoder);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("admin@test.com");
        assertThat(result.getPassword()).isEqualTo("encodedPassword");
        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
        assertThat(result.getName()).isEqualTo("Admin");
        assertThat(result.getSurname()).isEqualTo("User");
        assertThat(result.getCreditLimit()).isNull();
        assertThat(result.getUsedCreditLimit()).isNull();

        verify(passwordEncoder).encode("password123");
    }

    @Test
    @DisplayName("Should convert customer entity to register response")
    void shouldConvertCustomerEntityToRegisterResponse() {
        String jwtToken = "jwt-token-123";

        RegisterResponse result = AuthMapperUtil.toRegisterResponse(testCustomer, jwtToken);

        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo("jwt-token-123");
        assertThat(result.getMessage()).isEqualTo("User account created successfully with ID: 1");
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getUser()).isNotNull();
        assertThat(result.getUser().getName()).isEqualTo("John");
        assertThat(result.getUser().getSurname()).isEqualTo("Doe");
        assertThat(result.getUser().getEmail()).isEqualTo("customer@test.com");
        assertThat(result.getUser().getRole()).isEqualTo("CUSTOMER");
        assertThat(result.getRegistrationTime()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should convert customer entity to authentication response")
    void shouldConvertCustomerEntityToAuthenticationResponse() {
        String jwtToken = "auth-jwt-token-123";

        AuthenticationResponse result = AuthMapperUtil.toAuthenticationResponse(testCustomer, jwtToken);

        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo("auth-jwt-token-123");
        assertThat(result.getMessage()).isEqualTo("Successful login with customer ID: 1");
        assertThat(result.getTimestamp()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should generate JWT token with customer claims")
    void shouldGenerateJwtTokenWithCustomerClaims() {
        when(jwtUtil.generateToken(anyMap(), any(CustomerUserDetails.class))).thenReturn("generated-jwt-token");

        String result = AuthMapperUtil.generateJwtToken(testCustomer, jwtUtil);

        assertThat(result).isEqualTo("generated-jwt-token");
        
        verify(jwtUtil).generateToken(argThat(claims -> {
            Map<String, Object> claimsMap = (Map<String, Object>) claims;
            return "CUSTOMER".equals(claimsMap.get("role")) &&
                   Long.valueOf(1L).equals(claimsMap.get("customerId"));
        }), any(CustomerUserDetails.class));
    }

    @Test
    @DisplayName("Should create customer claims map correctly")
    void shouldCreateCustomerClaimsMapCorrectly() {
        Map<String, Object> result = AuthMapperUtil.createCustomerClaims(testCustomer);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get("role")).isEqualTo("CUSTOMER");
        assertThat(result.get("customerId")).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should generate registration message correctly")
    void shouldGenerateRegistrationMessageCorrectly() {
        String result = AuthMapperUtil.generateRegistrationMessage(testCustomer);

        assertThat(result).isEqualTo("User account created successfully with ID: 1");
    }

    @Test
    @DisplayName("Should generate authentication message correctly")
    void shouldGenerateAuthenticationMessageCorrectly() {
        String result = AuthMapperUtil.generateAuthenticationMessage(testCustomer);

        assertThat(result).isEqualTo("Successful login with customer ID: 1");
    }
} 
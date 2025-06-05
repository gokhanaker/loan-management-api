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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Authentication Service Tests")
class AuthenticationServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthenticationService authenticationService;

    private RegisterRequest customerRegisterRequest;
    private RegisterRequest adminRegisterRequest;
    private AuthenticationRequest validAuthenticationRequest;
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

        validAuthenticationRequest = AuthenticationRequest.builder()
                .email("customer@test.com")
                .password("password123")
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
    @DisplayName("Should successfully register a new customer")
    void shouldSuccessfullyRegisterNewCustomer() {
        Customer savedCustomer = Customer.builder()
                .id(1L)
                .email("customer@test.com")
                .password("encodedPassword")
                .role(Role.CUSTOMER)
                .name("John")
                .surname("Doe")
                .creditLimit(new BigDecimal("10000.00"))
                .usedCreditLimit(new BigDecimal("0.00"))
                .build();

        when(customerRepository.existsByEmail(customerRegisterRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(customerRegisterRequest.getPassword())).thenReturn("encodedPassword");
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);
        when(jwtUtil.generateToken(anyMap(), any())).thenReturn("jwt-token");

        RegisterResponse response = authenticationService.register(customerRegisterRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUser().getName()).isEqualTo("John");
        assertThat(response.getUser().getSurname()).isEqualTo("Doe");
        assertThat(response.getUser().getEmail()).isEqualTo("customer@test.com");
        assertThat(response.getUser().getRole()).isEqualTo("CUSTOMER");
        assertThat(response.getRegistrationTime()).isNotNull();
    }

    @Test
    @DisplayName("Should successfully register a new admin")
    void shouldSuccessfullyRegisterNewAdmin() {
        Customer savedAdmin = Customer.builder()
                .id(2L)
                .email("admin@test.com")
                .password("encodedPassword")
                .role(Role.ADMIN)
                .name("Admin")
                .surname("User")
                .build();

        when(customerRepository.existsByEmail(adminRegisterRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(adminRegisterRequest.getPassword())).thenReturn("encodedPassword");
        when(customerRepository.save(any(Customer.class))).thenReturn(savedAdmin);
        when(jwtUtil.generateToken(anyMap(), any())).thenReturn("admin-jwt-token");

        RegisterResponse response = authenticationService.register(adminRegisterRequest);

        assertThat(response.getToken()).isEqualTo("admin-jwt-token");
        assertThat(response.getUser().getName()).isEqualTo("Admin");
        assertThat(response.getUser().getSurname()).isEqualTo("User");
        assertThat(response.getUser().getEmail()).isEqualTo("admin@test.com");
        assertThat(response.getUser().getRole()).isEqualTo("ADMIN");
        assertThat(response.getRegistrationTime()).isNotNull();
    }

    @Test
    @DisplayName("Should throw EmailAlreadyExistsException when email already exists")
    void shouldThrowEmailAlreadyExistsExceptionWhenEmailExists() {
        when(customerRepository.existsByEmail(customerRegisterRequest.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.register(customerRegisterRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("Email already registered: customer@test.com");
    }

    @Test
    @DisplayName("Should properly encode password during registration")
    void shouldProperlyEncodePasswordDuringRegistration() {
        String rawPassword = "mySecretPassword123";
        String encodedPassword = "encoded_mySecretPassword123";
        
        RegisterRequest request = RegisterRequest.builder()
                .email("test@example.com")
                .password(rawPassword)
                .role(Role.CUSTOMER)
                .name("Test")
                .surname("User")
                .creditLimit(new BigDecimal("5000.00"))
                .usedCreditLimit(new BigDecimal("0.00"))
                .build();


        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);

        RegisterResponse response = authenticationService.register(request);

        assertThat(response).isNotNull();
        verify(passwordEncoder).encode(rawPassword);
    }

    @Test
    @DisplayName("Should successfully authenticate customer with valid credentials")
    void shouldSuccessfullyAuthenticateCustomerWithValidCredentials() {
        when(customerRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(testCustomer));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken(anyMap(), any())).thenReturn("generated-jwt-token");

        AuthenticationResponse result = authenticationService.authenticate(validAuthenticationRequest);

        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo("generated-jwt-token");
        assertThat(result.getMessage()).isEqualTo("Successful login with customer ID: 1!");
        assertThat(result.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should successfully authenticate admin with valid credentials")
    void shouldSuccessfullyAuthenticateAdminWithValidCredentials() {
        AuthenticationRequest adminAuthRequest = AuthenticationRequest.builder()
                .email("admin@test.com")
                .password("password123")
                .build();

        when(customerRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(testAdmin));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken(anyMap(), any())).thenReturn("admin-jwt-token");

        AuthenticationResponse result = authenticationService.authenticate(adminAuthRequest);

        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo("admin-jwt-token");
        assertThat(result.getMessage()).isEqualTo("Successful login with customer ID: 2!");
        assertThat(result.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when user does not exist")
    void shouldThrowUserNotFoundExceptionWhenUserDoesNotExist() {
        when(customerRepository.findByEmail("nonexistent@test.com")).thenReturn(Optional.empty());

        AuthenticationRequest nonExistentUserRequest = AuthenticationRequest.builder()
                .email("nonexistent@test.com")
                .password("password123")
                .build();

        assertThatThrownBy(() -> authenticationService.authenticate(nonExistentUserRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found with email: nonexistent@test.com");
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when password is incorrect")
    void shouldThrowInvalidCredentialsExceptionWhenPasswordIsIncorrect() {
        when(customerRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(testCustomer));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        AuthenticationRequest wrongPasswordRequest = AuthenticationRequest.builder()
                .email("customer@test.com")
                .password("wrongpassword")
                .build();

        assertThatThrownBy(() -> authenticationService.authenticate(wrongPasswordRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }
} 
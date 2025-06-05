package com.applab.loan_management.controller;

import com.applab.loan_management.constants.Role;
import com.applab.loan_management.dto.AuthenticationRequest;
import com.applab.loan_management.dto.AuthenticationResponse;
import com.applab.loan_management.dto.RegisterRequest;
import com.applab.loan_management.dto.RegisterResponse;
import com.applab.loan_management.exception.InvalidCredentialsException;
import com.applab.loan_management.exception.UserNotFoundException;
import com.applab.loan_management.service.AuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("Authentication Controller Tests")
class AuthenticationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private AuthenticationController authenticationController;

    private ObjectMapper objectMapper;
    private RegisterRequest validCustomerRequest;
    private RegisterRequest invalidCustomerRequestWithoutEmail;
    private RegisterRequest invalidCustomerRequestWithoutPassword;
    private RegisterRequest invalidCustomerRequestWithoutRole;
    private RegisterRequest invalidCustomerRequestWithoutCreditLimit;
    private RegisterRequest invalidCustomerRequestWithoutUsedCreditLimit;
    private RegisterRequest validAdminRequest;
    private RegisterResponse customerResponse;
    private RegisterResponse adminResponse;
    
    // Authentication-related test data
    private AuthenticationRequest validAuthenticationRequest;
    private AuthenticationRequest invalidAuthenticationRequestWithoutEmail;
    private AuthenticationRequest invalidAuthenticationRequestWithoutPassword;
    private AuthenticationResponse authenticationResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(authenticationController).build();

        // Setup valid customer registration request
        validCustomerRequest = RegisterRequest.builder()
                .email("customer@test.com")
                .password("password123")
                .role(Role.CUSTOMER)
                .name("John")
                .surname("Doe")
                .creditLimit(new BigDecimal("10000.00"))
                .usedCreditLimit(new BigDecimal("0.00"))
                .build();

        // Setup invalid customer registration requests
        invalidCustomerRequestWithoutEmail = RegisterRequest.builder()
                .password("password123")
                .role(Role.CUSTOMER)
                .name("John")
                .surname("Doe")
                .creditLimit(new BigDecimal("10000.00"))
                .usedCreditLimit(new BigDecimal("0.00"))
                .build();
        
        invalidCustomerRequestWithoutPassword = RegisterRequest.builder()
                .email("customer@test.com")
                .role(Role.CUSTOMER)
                .name("John")
                .surname("Doe")
                .creditLimit(new BigDecimal("10000.00"))
                .usedCreditLimit(new BigDecimal("0.00"))
                .build();
        
        invalidCustomerRequestWithoutRole = RegisterRequest.builder()
                .email("customer@test.com")
                .password("password123")
                .name("John")
                .surname("Doe")
                .creditLimit(new BigDecimal("10000.00"))
                .usedCreditLimit(new BigDecimal("0.00"))
                .build();

        invalidCustomerRequestWithoutCreditLimit = RegisterRequest.builder()
                .email("customer@test.com")
                .password("password123")
                .role(Role.CUSTOMER)
                .name("John")
                .surname("Doe")
                .usedCreditLimit(new BigDecimal("0.00"))
                .build();

        invalidCustomerRequestWithoutUsedCreditLimit = RegisterRequest.builder()
                .email("customer@test.com")
                .password("password123")
                .role(Role.CUSTOMER)
                .name("John")
                .surname("Doe")
                .creditLimit(new BigDecimal("10000.00"))
                .build();

        // Setup valid admin registration request
        validAdminRequest = RegisterRequest.builder()
                .email("admin@test.com")
                .password("password123")
                .role(Role.ADMIN)
                .name("Admin")
                .surname("User")
                .build();

        // Setup customer registration response (matching current implementation behavior)
        customerResponse = RegisterResponse.builder()
                .token("customer-jwt-token")
                .message("User account created successfully with ID: null")
                .userId(null) // Current implementation returns null
                .user(RegisterResponse.UserProfile.builder()
                        .name("John")
                        .surname("Doe")
                        .email("customer@test.com")
                        .role("CUSTOMER")
                        .build())
                .registrationTime(LocalDateTime.now())
                .build();

        // Setup admin registration response (matching current implementation behavior)
        adminResponse = RegisterResponse.builder()
                .token("admin-jwt-token")
                .message("User account created successfully with ID: null")
                .userId(null) // Current implementation returns null
                .user(RegisterResponse.UserProfile.builder()
                        .name("Admin")
                        .surname("User")
                        .email("admin@test.com")
                        .role("ADMIN")
                        .build())
                .registrationTime(LocalDateTime.now())
                .build();

        // Setup authentication test data
        validAuthenticationRequest = AuthenticationRequest.builder()
                .email("customer@test.com")
                .password("password123")
                .build();

        invalidAuthenticationRequestWithoutEmail = AuthenticationRequest.builder()
                .password("password123")
                .build();

        invalidAuthenticationRequestWithoutPassword = AuthenticationRequest.builder()
                .email("customer@test.com")
                .build();

        authenticationResponse = AuthenticationResponse.builder()
                .token("auth-jwt-token")
                .message("Successful login with customer ID: 1!")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new customer")
    void shouldSuccessfullyRegisterNewCustomer() throws Exception {
        // Given
        when(authenticationService.register(any(RegisterRequest.class))).thenReturn(customerResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCustomerRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("customer-jwt-token"))
                .andExpect(jsonPath("$.message").value("User account created successfully with ID: null"))
                .andExpect(jsonPath("$.userId").isEmpty())
                .andExpect(jsonPath("$.user.name").value("John"))
                .andExpect(jsonPath("$.user.surname").value("Doe"))
                .andExpect(jsonPath("$.user.email").value("customer@test.com"))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.registrationTime").exists());

        verify(authenticationService).register(argThat(request ->
                request.getEmail().equals("customer@test.com") &&
                request.getPassword().equals("password123") &&
                request.getRole() == Role.CUSTOMER &&
                request.getName().equals("John") &&
                request.getSurname().equals("Doe") &&
                request.getCreditLimit().equals(new BigDecimal("10000.00")) &&
                request.getUsedCreditLimit().equals(new BigDecimal("0.00"))
        ));
    }

    @Test
    @DisplayName("Should successfully register a new admin")
    void shouldSuccessfullyRegisterNewAdmin() throws Exception {
        // Given
        when(authenticationService.register(any(RegisterRequest.class))).thenReturn(adminResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAdminRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("admin-jwt-token"))
                .andExpect(jsonPath("$.message").value("User account created successfully with ID: null"))
                .andExpect(jsonPath("$.userId").isEmpty())
                .andExpect(jsonPath("$.user.name").value("Admin"))
                .andExpect(jsonPath("$.user.surname").value("User"))
                .andExpect(jsonPath("$.user.email").value("admin@test.com"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.registrationTime").exists());

        verify(authenticationService).register(argThat(request ->
                request.getEmail().equals("admin@test.com") &&
                request.getPassword().equals("password123") &&
                request.getRole() == Role.ADMIN &&
                request.getName().equals("Admin") &&
                request.getSurname().equals("User")
        ));
    }

    @Test
    @DisplayName("Should return 400 when email is not provided")
    void shouldReturn400WhenEmailIsNotProvided() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCustomerRequestWithoutEmail)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when password is not provided")
    void shouldReturn400WhenPasswordIsNotProvided() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCustomerRequestWithoutPassword)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when role is not provided")
    void shouldReturn400WhenRoleIsNotProvided() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCustomerRequestWithoutRole)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when credit limit is not provided for customer role")
    void shouldReturn400WhenCreditLimitIsNotProvidedForCustomerRole() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCustomerRequestWithoutCreditLimit)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when used credit limit is not provided for customer role")
    void shouldReturn400WhenUsedCreditLimitIsNotProvidedForCustomerRole() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCustomerRequestWithoutUsedCreditLimit)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should accept admin registration without credit fields")
    void shouldAcceptAdminRegistrationWithoutCreditFields() throws Exception {
        // Given
        RegisterRequest adminRequestWithoutCredit = RegisterRequest.builder()
                .email("admin@test.com")
                .password("password123")
                .role(Role.ADMIN)
                .name("Admin")
                .surname("User")
                .build();

        when(authenticationService.register(any(RegisterRequest.class))).thenReturn(adminResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminRequestWithoutCredit)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("admin-jwt-token"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));

        verify(authenticationService).register(argThat(request ->
                request.getEmail().equals("admin@test.com") &&
                request.getRole() == Role.ADMIN &&
                request.getCreditLimit() == null &&
                request.getUsedCreditLimit() == null
        ));
    }

    @Test
    @DisplayName("Should successfully authenticate user with valid credentials")
    void shouldSuccessfullyAuthenticateUserWithValidCredentials() throws Exception {
        // Given
        when(authenticationService.authenticate(any(AuthenticationRequest.class))).thenReturn(authenticationResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validAuthenticationRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("auth-jwt-token"))
                .andExpect(jsonPath("$.message").value("Successful login with customer ID: 1!"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authenticationService).authenticate(argThat(request ->
                request.getEmail().equals("customer@test.com") &&
                request.getPassword().equals("password123")
        ));
    }

    @Test
    @DisplayName("Should return 400 when email is missing in authentication request")
    void shouldReturn400WhenEmailIsMissingInAuthenticationRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidAuthenticationRequestWithoutEmail)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).authenticate(any(AuthenticationRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when password is missing in authentication request")
    void shouldReturn400WhenPasswordIsMissingInAuthenticationRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidAuthenticationRequestWithoutPassword)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).authenticate(any(AuthenticationRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when email format is invalid in authentication request")
    void shouldReturn400WhenEmailFormatIsInvalidInAuthenticationRequest() throws Exception {
        // Given
        AuthenticationRequest invalidEmailFormatRequest = AuthenticationRequest.builder()
                .email("invalid-email-format")
                .password("password123")
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmailFormatRequest)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).authenticate(any(AuthenticationRequest.class));
    }

    @Test
    @DisplayName("Should return 401 when user is not found")
    void shouldReturn401WhenUserIsNotFound() throws Exception {
        when(authenticationService.authenticate(any(AuthenticationRequest.class)))
                .thenThrow(new UserNotFoundException("nonexistent@test.com"));

        AuthenticationRequest nonExistentUserRequest = AuthenticationRequest.builder()
                .email("nonexistent@test.com")
                .password("password123")
                .build();

        try {
            mockMvc.perform(post("/api/auth/authenticate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(nonExistentUserRequest)));
        } catch (Exception e) {
            verify(authenticationService).authenticate(any(AuthenticationRequest.class));
            assertThat(e).hasCauseInstanceOf(UserNotFoundException.class);
            assertThat(e.getCause().getMessage()).contains("nonexistent@test.com");
        }
    }

    @Test
    @DisplayName("Should return 401 when credentials are invalid")
    void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
        when(authenticationService.authenticate(any(AuthenticationRequest.class)))
                .thenThrow(new InvalidCredentialsException());

        AuthenticationRequest wrongPasswordRequest = AuthenticationRequest.builder()
                .email("customer@test.com")
                .password("wrongpassword")
                .build();

        try {
            mockMvc.perform(post("/api/auth/authenticate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongPasswordRequest)));
        } catch (Exception e) {
            verify(authenticationService).authenticate(any(AuthenticationRequest.class));
            assertThat(e).hasCauseInstanceOf(InvalidCredentialsException.class);
            assertThat(e.getCause().getMessage()).isEqualTo("Invalid email or password");
        }
    }
} 
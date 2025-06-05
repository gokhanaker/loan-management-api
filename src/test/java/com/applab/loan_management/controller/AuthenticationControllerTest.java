package com.applab.loan_management.controller;

import com.applab.loan_management.constants.Role;
import com.applab.loan_management.dto.RegisterRequest;
import com.applab.loan_management.dto.RegisterResponse;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
} 
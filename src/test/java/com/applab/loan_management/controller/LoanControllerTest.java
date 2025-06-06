package com.applab.loan_management.controller;

import com.applab.loan_management.dto.CreateLoanRequest;
import com.applab.loan_management.dto.CreateLoanResponse;
import com.applab.loan_management.dto.LoanListResponse;
import com.applab.loan_management.dto.LoanInstallmentResponse;
import com.applab.loan_management.dto.PayLoanRequest;
import com.applab.loan_management.dto.PayLoanResponse;
import com.applab.loan_management.entity.Customer;
import com.applab.loan_management.entity.Loan;
import com.applab.loan_management.service.LoanService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Loan Controller Tests")
class LoanControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LoanService loanService;

    @InjectMocks
    private LoanController loanController;

    private ObjectMapper objectMapper;
    private CreateLoanRequest validCreateLoanRequest;
    private CreateLoanResponse createLoanResponse;
    private LoanListResponse loanListResponse;
    private LoanInstallmentResponse installmentResponse;
    private PayLoanRequest validPayLoanRequest;
    private PayLoanResponse payLoanResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(loanController).build();

        validCreateLoanRequest = CreateLoanRequest.builder()
                .customerId(1L)
                .amount(new BigDecimal("10000.00"))
                .interestRate(new BigDecimal("0.2"))
                .numberOfInstallments(12)
                .build();

        createLoanResponse = CreateLoanResponse.builder()
                .id(1L)
                .customerId(1L)
                .customerName("John")
                .customerSurname("Doe")
                .loanAmount(new BigDecimal("10000.00"))
                .interestRate(new BigDecimal("0.2"))
                .numberOfInstallments(12)
                .createDate(LocalDateTime.now())
                .isPaid(false)
                .totalAmount(new BigDecimal("12000.00"))
                .build();

        loanListResponse = LoanListResponse.builder()
                .id(1L)
                .customerId(1L)
                .customerName("John")
                .customerSurname("Doe")
                .loanAmount(new BigDecimal("10000.00"))
                .interestRate(new BigDecimal("0.2"))
                .numberOfInstallments(12)
                .createDate(LocalDateTime.now())
                .isPaid(false)
                .totalAmount(new BigDecimal("12000.00"))
                .remainingInstallments(10)
                .build();

        installmentResponse = LoanInstallmentResponse.builder()
                .id(1L)
                .loanId(1L)
                .amount(new BigDecimal("1000.00"))
                .dueDate(LocalDate.now().plusMonths(1))
                .paymentDate(null)
                .paidAmount(null)
                .isPaid(false)
                .installmentNumber(1)
                .build();

        validPayLoanRequest = PayLoanRequest.builder()
                .amount(new BigDecimal("2000.00"))
                .build();

        payLoanResponse = PayLoanResponse.builder()
                .installmentsPaid(2)
                .totalAmountSpent(new BigDecimal("2000.00"))
                .isLoanFullyPaid(false)
                .message("Successfully paid 2 installment(s) for a total of 2000.00")
                .build();
    }

    @Test
    @DisplayName("Should successfully create a loan")
    void shouldSuccessfullyCreateLoan() throws Exception {
        Customer mockCustomer = Customer.builder()
                .id(1L)
                .name("John")
                .surname("Doe")
                .email("john@test.com")
                .build();
        
        Loan mockLoan = Loan.builder()
                .id(1L)
                .customer(mockCustomer)
                .loanAmount(new BigDecimal("10000.00"))
                .interestRate(new BigDecimal("0.2"))
                .numberOfInstallments(12)
                .createDate(LocalDateTime.now())
                .isPaid(false)
                .build();

        when(loanService.createLoan(any(CreateLoanRequest.class))).thenReturn(mockLoan);

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateLoanRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.customerId").value(1L))
                .andExpect(jsonPath("$.customerName").value("John"))
                .andExpect(jsonPath("$.customerSurname").value("Doe"))
                .andExpect(jsonPath("$.loanAmount").value(10000.00))
                .andExpect(jsonPath("$.interestRate").value(0.2))
                .andExpect(jsonPath("$.numberOfInstallments").value(12))
                .andExpect(jsonPath("$.isPaid").value(false));
    }

    @Test
    @DisplayName("Should return 400 for invalid loan creation request")
    void shouldReturn400ForInvalidLoanCreationRequest() throws Exception {
        CreateLoanRequest invalidRequest = CreateLoanRequest.builder()
                .customerId(null)
                .amount(new BigDecimal("50.00")) // Below minimum
                .interestRate(new BigDecimal("0.6")) // Above maximum
                .numberOfInstallments(5) // Invalid value
                .build();

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(loanService, never()).createLoan(any(CreateLoanRequest.class));
    }

    @Test
    @DisplayName("Should successfully list loans with filters")
    void shouldSuccessfullyListLoansWithFilters() throws Exception {
        List<LoanListResponse> mockLoans = Arrays.asList(loanListResponse);
        when(loanService.listLoans(1L, false, 12)).thenReturn(mockLoans);

        mockMvc.perform(get("/api/loans")
                        .param("customerId", "1")
                        .param("isPaid", "false")
                        .param("numberOfInstallments", "12"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].customerId").value(1L))
                .andExpect(jsonPath("$[0].loanAmount").value(10000.00))
                .andExpect(jsonPath("$[0].isPaid").value(false))
                .andExpect(jsonPath("$[0].remainingInstallments").value(10));

        verify(loanService).listLoans(1L, false, 12);
    }

    @Test
    @DisplayName("Should successfully list loan installments")
    void shouldSuccessfullyListLoanInstallments() throws Exception {
        List<LoanInstallmentResponse> mockInstallments = Arrays.asList(installmentResponse);
        when(loanService.listLoanInstallments(1L)).thenReturn(mockInstallments);

        mockMvc.perform(get("/api/loans/1/installments"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].loanId").value(1L))
                .andExpect(jsonPath("$[0].amount").value(1000.00))
                .andExpect(jsonPath("$[0].isPaid").value(false))
                .andExpect(jsonPath("$[0].installmentNumber").value(1));

        verify(loanService).listLoanInstallments(1L);
    }

    @Test
    @DisplayName("Should successfully pay loan")
    void shouldSuccessfullyPayLoan() throws Exception {
        when(loanService.payLoan(eq(1L), any(PayLoanRequest.class))).thenReturn(payLoanResponse);

        mockMvc.perform(post("/api/loans/1/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayLoanRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.installmentsPaid").value(2))
                .andExpect(jsonPath("$.totalAmountSpent").value(2000.00))
                .andExpect(jsonPath("$.loanFullyPaid").value(false))
                .andExpect(jsonPath("$.message").value("Successfully paid 2 installment(s) for a total of 2000.00"));

        verify(loanService).payLoan(eq(1L), argThat(request ->
                request.getAmount().equals(new BigDecimal("2000.00"))
        ));
    }
} 
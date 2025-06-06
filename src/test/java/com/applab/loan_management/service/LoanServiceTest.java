package com.applab.loan_management.service;

import com.applab.loan_management.constants.Role;
import com.applab.loan_management.dto.CreateLoanRequest;
import com.applab.loan_management.dto.LoanListResponse;
import com.applab.loan_management.dto.LoanInstallmentResponse;
import com.applab.loan_management.dto.PayLoanRequest;
import com.applab.loan_management.dto.PayLoanResponse;
import com.applab.loan_management.entity.Customer;
import com.applab.loan_management.entity.Loan;
import com.applab.loan_management.entity.LoanInstallment;
import com.applab.loan_management.exception.AdminCannotCreateLoanException;
import com.applab.loan_management.exception.CustomerNotFoundException;
import com.applab.loan_management.exception.InsufficientCreditLimitException;
import com.applab.loan_management.exception.InvalidParameterException;
import com.applab.loan_management.exception.LoanAlreadyPaidException;
import com.applab.loan_management.repository.CustomerRepository;
import com.applab.loan_management.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Loan Service Tests")
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private LoanService loanService;

    private Customer testCustomer;
    private CreateLoanRequest validLoanRequest;
    private Loan testLoan;

    @BeforeEach
    void setUp() {
        testCustomer = Customer.builder()
                .id(1L)
                .email("customer@test.com")
                .name("John")
                .surname("Doe")
                .role(Role.CUSTOMER)
                .creditLimit(new BigDecimal("50000.00"))
                .usedCreditLimit(new BigDecimal("10000.00"))
                .build();

        validLoanRequest = CreateLoanRequest.builder()
                .customerId(1L)
                .amount(new BigDecimal("10000.00"))
                .interestRate(new BigDecimal("0.2"))
                .numberOfInstallments(12)
                .build();

        testLoan = Loan.builder()
                .id(1L)
                .customer(testCustomer)
                .loanAmount(new BigDecimal("10000.00"))
                .interestRate(new BigDecimal("0.2"))
                .numberOfInstallments(12)
                .createDate(LocalDateTime.now())
                .isPaid(false)
                .build();

        List<LoanInstallment> installments = Arrays.asList(
                createTestInstallment(1L, testLoan, new BigDecimal("1000.00"), LocalDate.now().plusMonths(1), false),
                createTestInstallment(2L, testLoan, new BigDecimal("1000.00"), LocalDate.now().plusMonths(2), false),
                createTestInstallment(3L, testLoan, new BigDecimal("1000.00"), LocalDate.now().plusMonths(3), true)
        );
        testLoan.setInstallments(installments);
    }

    private LoanInstallment createTestInstallment(Long id, Loan loan, BigDecimal amount, LocalDate dueDate, boolean isPaid) {
        return LoanInstallment.builder()
                .id(id)
                .loan(loan)
                .amount(amount)
                .dueDate(dueDate)
                .isPaid(isPaid)
                .build();
    }

    @Test
    @DisplayName("Should successfully create a loan when all conditions are met")
    void shouldSuccessfullyCreateLoan() {
        Loan savedLoan = Loan.builder()
                .id(1L)
                .customer(testCustomer)
                .loanAmount(validLoanRequest.getAmount())
                .interestRate(validLoanRequest.getInterestRate())
                .numberOfInstallments(validLoanRequest.getNumberOfInstallments())
                .createDate(LocalDateTime.now())
                .isPaid(false)
                .build();

        when(currentUserService.canAccessCustomerData(1L)).thenReturn(true);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);
        when(loanRepository.save(any(Loan.class))).thenReturn(savedLoan);

        Loan result = loanService.createLoan(validLoanRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getLoanAmount()).isEqualTo(new BigDecimal("10000.00"));
        assertThat(result.getInterestRate()).isEqualTo(new BigDecimal("0.2"));
        assertThat(result.getNumberOfInstallments()).isEqualTo(12);
        assertThat(result.getIsPaid()).isFalse();
    }

    @Test
    @DisplayName("Should throw CustomerNotFoundException when customer does not exist")
    void shouldThrowCustomerNotFoundExceptionWhenCustomerDoesNotExist() {
        when(currentUserService.canAccessCustomerData(1L)).thenReturn(true);
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.createLoan(validLoanRequest))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessage("Customer not found with ID: 1");

        verify(currentUserService).canAccessCustomerData(1L);
        verify(customerRepository).findById(1L);
        verify(customerRepository, never()).save(any(Customer.class));
        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    @DisplayName("Should throw InsufficientCreditLimitException when credit limit is exceeded")
    void shouldThrowInsufficientCreditLimitExceptionWhenCreditLimitExceeded() {
        Customer customerWithLowCredit = Customer.builder()
                .id(1L)
                .email("customer@test.com")
                .name("John")
                .surname("Doe")
                .role(Role.CUSTOMER)
                .creditLimit(new BigDecimal("15000.00"))
                .usedCreditLimit(new BigDecimal("10000.00"))
                .build();

        when(currentUserService.canAccessCustomerData(1L)).thenReturn(true);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customerWithLowCredit));

        assertThatThrownBy(() -> loanService.createLoan(validLoanRequest))
                .isInstanceOf(InsufficientCreditLimitException.class);

        verify(currentUserService).canAccessCustomerData(1L);
        verify(customerRepository).findById(1L);
        verify(customerRepository, never()).save(any(Customer.class));
        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    @DisplayName("Should throw AdminCannotCreateLoanException when customer is admin")
    void shouldThrowAdminCannotCreateLoanExceptionWhenCustomerIsAdmin() {
        Customer adminCustomer = Customer.builder()
                .id(1L)
                .email("admin@test.com")
                .name("Admin")
                .surname("User")
                .role(Role.ADMIN)
                .creditLimit(null)
                .usedCreditLimit(null)
                .build();

        when(currentUserService.canAccessCustomerData(1L)).thenReturn(true);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(adminCustomer));

        assertThatThrownBy(() -> loanService.createLoan(validLoanRequest))
                .isInstanceOf(AdminCannotCreateLoanException.class);

        verify(currentUserService).canAccessCustomerData(1L);
        verify(customerRepository).findById(1L);
        verify(customerRepository, never()).save(any(Customer.class));
        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    @DisplayName("Should successfully list loans with filters")
    void shouldSuccessfullyListLoansWithFilters() {
        List<Loan> mockLoans = Arrays.asList(testLoan);
        
        when(currentUserService.canAccessCustomerData(1L)).thenReturn(true);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(loanRepository.findByCustomerIdAndIsPaid(1L, false)).thenReturn(mockLoans);

        List<LoanListResponse> result = loanService.listLoans(1L, false, null);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getCustomerId()).isEqualTo(1L);
        assertThat(result.get(0).getLoanAmount()).isEqualTo(new BigDecimal("10000.00"));
        assertThat(result.get(0).getIsPaid()).isFalse();

        verify(currentUserService).canAccessCustomerData(1L);
        verify(customerRepository).findById(1L);
        verify(loanRepository).findByCustomerIdAndIsPaid(1L, false);
    }

    @Test
    @DisplayName("Should throw InvalidParameterException for invalid customerId in listLoans")
    void shouldThrowInvalidParameterExceptionForInvalidCustomerIdInListLoans() {
        assertThatThrownBy(() -> loanService.listLoans(-1L, null, null))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessage("Invalid parameter 'customerId': must be a positive number");

        verify(currentUserService, never()).canAccessCustomerData(anyLong());
        verify(customerRepository, never()).findById(anyLong());
        verify(loanRepository, never()).findByCustomerId(anyLong());
    }

    @Test
    @DisplayName("Should successfully list loan installments")
    void shouldSuccessfullyListLoanInstallments() {
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
        when(currentUserService.canAccessCustomerData(1L)).thenReturn(true);

        List<LoanInstallmentResponse> result = loanService.listLoanInstallments(1L);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getLoanId()).isEqualTo(1L);
        assertThat(result.get(0).getAmount()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(result.get(0).getIsPaid()).isFalse();
        assertThat(result.get(2).getIsPaid()).isTrue();

        verify(loanRepository).findById(1L);
        verify(currentUserService).canAccessCustomerData(1L);
    }

    @Test
    @DisplayName("Should successfully pay loan installments")
    void shouldSuccessfullyPayLoanInstallments() {
        PayLoanRequest payRequest = PayLoanRequest.builder()
                .amount(new BigDecimal("2500.00"))
                .build();

        List<LoanInstallment> installments = Arrays.asList(
                createTestInstallment(1L, testLoan, new BigDecimal("1000.00"), LocalDate.now().plusMonths(1), false),
                createTestInstallment(2L, testLoan, new BigDecimal("1000.00"), LocalDate.now().plusMonths(2), false),
                createTestInstallment(3L, testLoan, new BigDecimal("1000.00"), LocalDate.now().plusMonths(3), false)
        );
        testLoan.setInstallments(installments);

        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
        when(currentUserService.canAccessCustomerData(1L)).thenReturn(true);
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);
        when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);

        PayLoanResponse result = loanService.payLoan(1L, payRequest);

        assertThat(result).isNotNull();
        assertThat(result.getInstallmentsPaid()).isEqualTo(2);
        assertThat(result.getTotalAmountSpent()).isEqualTo(new BigDecimal("2000.00"));
        assertThat(result.isLoanFullyPaid()).isFalse();
        assertThat(result.getMessage()).contains("Successfully paid 2 installment(s)");

        verify(loanRepository).findById(1L);
        verify(currentUserService).canAccessCustomerData(1L);
        verify(customerRepository).save(any(Customer.class));
        verify(loanRepository).save(any(Loan.class));
    }

    @Test
    @DisplayName("Should throw LoanAlreadyPaidException when trying to pay an already paid loan")
    void shouldThrowLoanAlreadyPaidExceptionWhenTryingToPayAlreadyPaidLoan() {
        Loan paidLoan = Loan.builder()
                .id(1L)
                .customer(testCustomer)
                .loanAmount(new BigDecimal("10000.00"))
                .interestRate(new BigDecimal("0.2"))
                .numberOfInstallments(12)
                .createDate(LocalDateTime.now())
                .isPaid(true)
                .build();

        PayLoanRequest payRequest = PayLoanRequest.builder()
                .amount(new BigDecimal("1000.00"))
                .build();

        when(loanRepository.findById(1L)).thenReturn(Optional.of(paidLoan));
        when(currentUserService.canAccessCustomerData(1L)).thenReturn(true);

        assertThatThrownBy(() -> loanService.payLoan(1L, payRequest))
                .isInstanceOf(LoanAlreadyPaidException.class)
                .hasMessage("Loan with ID 1 is already fully paid");

        verify(loanRepository).findById(1L);
        verify(currentUserService).canAccessCustomerData(1L);
        verify(customerRepository, never()).save(any(Customer.class));
        verify(loanRepository, never()).save(any(Loan.class));
    }
} 
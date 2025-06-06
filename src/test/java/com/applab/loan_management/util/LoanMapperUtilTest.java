package com.applab.loan_management.util;

import com.applab.loan_management.constants.Role;
import com.applab.loan_management.dto.CreateLoanResponse;
import com.applab.loan_management.dto.LoanInstallmentResponse;
import com.applab.loan_management.dto.LoanListResponse;
import com.applab.loan_management.entity.Customer;
import com.applab.loan_management.entity.Loan;
import com.applab.loan_management.entity.LoanInstallment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("LoanMapperUtil Tests")
class LoanMapperUtilTest {

    private Customer testCustomer;
    private Loan testLoan;
    private List<LoanInstallment> testInstallments;

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

        testLoan = Loan.builder()
                .id(1L)
                .customer(testCustomer)
                .loanAmount(new BigDecimal("10000.00"))
                .interestRate(new BigDecimal("0.2"))
                .numberOfInstallments(12)
                .createDate(LocalDateTime.now())
                .isPaid(false)
                .build();

        testInstallments = Arrays.asList(
                createTestInstallment(1L, testLoan, new BigDecimal("1000.00"), LocalDate.now().plusMonths(1), false, null, null),
                createTestInstallment(2L, testLoan, new BigDecimal("1000.00"), LocalDate.now().plusMonths(2), true, new BigDecimal("1000.00"), LocalDate.now()),
                createTestInstallment(3L, testLoan, new BigDecimal("1000.00"), LocalDate.now().plusMonths(3), false, null, null)
        );
        testLoan.setInstallments(testInstallments);
    }

    private LoanInstallment createTestInstallment(Long id, Loan loan, BigDecimal amount, LocalDate dueDate, 
                                                 boolean isPaid, BigDecimal paidAmount, LocalDate paymentDate) {
        return LoanInstallment.builder()
                .id(id)
                .loan(loan)
                .amount(amount)
                .dueDate(dueDate)
                .isPaid(isPaid)
                .paidAmount(paidAmount)
                .paymentDate(paymentDate)
                .build();
    }

    @Test
    @DisplayName("Should convert loan entity to CreateLoanResponse DTO")
    void shouldConvertLoanEntityToCreateLoanResponse() {
        CreateLoanResponse result = LoanMapperUtil.toCreateLoanResponse(testLoan);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCustomerId()).isEqualTo(1L);
        assertThat(result.getCustomerName()).isEqualTo("John");
        assertThat(result.getCustomerSurname()).isEqualTo("Doe");
        assertThat(result.getLoanAmount()).isEqualTo(new BigDecimal("10000.00"));
        assertThat(result.getInterestRate()).isEqualTo(new BigDecimal("0.2"));
        assertThat(result.getNumberOfInstallments()).isEqualTo(12);
        assertThat(result.getCreateDate()).isEqualTo(testLoan.getCreateDate());
        assertThat(result.getIsPaid()).isFalse();
        assertThat(result.getTotalAmount()).isEqualTo(new BigDecimal("12000.00")); // 10000 * (1 + 0.2)
    }

    @Test
    @DisplayName("Should convert loan entity to LoanListResponse DTO")
    void shouldConvertLoanEntityToLoanListResponse() {
        LoanListResponse result = LoanMapperUtil.toLoanListResponse(testLoan);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCustomerId()).isEqualTo(1L);
        assertThat(result.getCustomerName()).isEqualTo("John");
        assertThat(result.getCustomerSurname()).isEqualTo("Doe");
        assertThat(result.getLoanAmount()).isEqualTo(new BigDecimal("10000.00"));
        assertThat(result.getInterestRate()).isEqualTo(new BigDecimal("0.2"));
        assertThat(result.getNumberOfInstallments()).isEqualTo(12);
        assertThat(result.getCreateDate()).isEqualTo(testLoan.getCreateDate());
        assertThat(result.getIsPaid()).isFalse();
        assertThat(result.getTotalAmount()).isEqualTo(new BigDecimal("12000.00"));
        assertThat(result.getRemainingInstallments()).isEqualTo(2); // 2 unpaid installments
    }

    @Test
    @DisplayName("Should convert loan installment entity to LoanInstallmentResponse DTO")
    void shouldConvertLoanInstallmentEntityToLoanInstallmentResponse() {
        LoanInstallment paidInstallment = testInstallments.get(1);
        int installmentNumber = 2;

        LoanInstallmentResponse result = LoanMapperUtil.toLoanInstallmentResponse(paidInstallment, installmentNumber);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getLoanId()).isEqualTo(1L);
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(result.getDueDate()).isEqualTo(paidInstallment.getDueDate());
        assertThat(result.getPaymentDate()).isEqualTo(paidInstallment.getPaymentDate());
        assertThat(result.getPaidAmount()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(result.getIsPaid()).isTrue();
        assertThat(result.getInstallmentNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should convert list of loan installments to list of LoanInstallmentResponse DTOs")
    void shouldConvertListOfLoanInstallmentsToListOfLoanInstallmentResponseDTOs() {
        List<LoanInstallmentResponse> result = LoanMapperUtil.toLoanInstallmentResponseList(testInstallments);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        
        // Check first installment
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getInstallmentNumber()).isEqualTo(1);
        assertThat(result.get(0).getIsPaid()).isFalse();
        
        // Check second installment (paid)
        assertThat(result.get(1).getId()).isEqualTo(2L);
        assertThat(result.get(1).getInstallmentNumber()).isEqualTo(2);
        assertThat(result.get(1).getIsPaid()).isTrue();
        assertThat(result.get(1).getPaidAmount()).isEqualTo(new BigDecimal("1000.00"));
        
        // Check third installment
        assertThat(result.get(2).getId()).isEqualTo(3L);
        assertThat(result.get(2).getInstallmentNumber()).isEqualTo(3);
        assertThat(result.get(2).getIsPaid()).isFalse();
    }

    @Test
    @DisplayName("Should calculate total loan amount with interest correctly")
    void shouldCalculateTotalLoanAmountWithInterestCorrectly() {
        BigDecimal loanAmount = new BigDecimal("10000.00");
        BigDecimal interestRate = new BigDecimal("0.15");
        BigDecimal result = LoanMapperUtil.calculateTotalLoanAmount(loanAmount, interestRate);

        assertThat(result).isEqualTo(new BigDecimal("11500.00")); // 10000 * (1 + 0.15)
    }

    @Test
    @DisplayName("Should calculate total loan amount with zero interest")
    void shouldCalculateTotalLoanAmountWithZeroInterest() {
        BigDecimal loanAmount = new BigDecimal("5000.00");
        BigDecimal interestRate = new BigDecimal("0.0");
        BigDecimal result = LoanMapperUtil.calculateTotalLoanAmount(loanAmount, interestRate);

        assertThat(result).isEqualTo(new BigDecimal("5000.00"));
    }

    @Test
    @DisplayName("Should calculate installment amount correctly")
    void shouldCalculateInstallmentAmountCorrectly() {
        BigDecimal totalAmount = new BigDecimal("12000.00");
        int numberOfInstallments = 12;
        BigDecimal result = LoanMapperUtil.calculateInstallmentAmount(totalAmount, numberOfInstallments);

        assertThat(result).isEqualTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("Should calculate remaining installments correctly")
    void shouldCalculateRemainingInstallmentsCorrectly() {
        int result = LoanMapperUtil.calculateRemainingInstallments(testInstallments);

        assertThat(result).isEqualTo(2); // 2 unpaid installments out of 3
    }

    @Test
    @DisplayName("Should return zero remaining installments when all are paid")
    void shouldReturnZeroRemainingInstallmentsWhenAllArePaid() {
        List<LoanInstallment> allPaidInstallments = Arrays.asList(
                createTestInstallment(1L, testLoan, new BigDecimal("1000.00"), LocalDate.now().plusMonths(1), true, new BigDecimal("1000.00"), LocalDate.now()),
                createTestInstallment(2L, testLoan, new BigDecimal("1000.00"), LocalDate.now().plusMonths(2), true, new BigDecimal("1000.00"), LocalDate.now()),
                createTestInstallment(3L, testLoan, new BigDecimal("1000.00"), LocalDate.now().plusMonths(3), true, new BigDecimal("1000.00"), LocalDate.now())
        );

        int result = LoanMapperUtil.calculateRemainingInstallments(allPaidInstallments);

        assertThat(result).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return true when loan is fully paid")
    void shouldReturnTrueWhenLoanIsFullyPaid() {
        List<LoanInstallment> allPaidInstallments = Arrays.asList(
                createTestInstallment(1L, testLoan, new BigDecimal("1000.00"), LocalDate.now().plusMonths(1), true, new BigDecimal("1000.00"), LocalDate.now()),
                createTestInstallment(2L, testLoan, new BigDecimal("1000.00"), LocalDate.now().plusMonths(2), true, new BigDecimal("1000.00"), LocalDate.now())
        );

        boolean result = LoanMapperUtil.isLoanFullyPaid(allPaidInstallments);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when loan is not fully paid")
    void shouldReturnFalseWhenLoanIsNotFullyPaid() {
        boolean result = LoanMapperUtil.isLoanFullyPaid(testInstallments);

        assertThat(result).isFalse();
    }
} 
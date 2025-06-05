package com.applab.loan_management.util;

import com.applab.loan_management.dto.CreateLoanResponse;
import com.applab.loan_management.dto.LoanInstallmentResponse;
import com.applab.loan_management.dto.LoanListResponse;
import com.applab.loan_management.entity.Loan;
import com.applab.loan_management.entity.LoanInstallment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;


/* Utility class for converting loan entities to DTOs 
 * and performing loan-related calculations
 */
public final class LoanMapperUtil {

    private LoanMapperUtil() {}

    // Converts a Loan entity to CreateLoanResponse DTO
    public static CreateLoanResponse toCreateLoanResponse(Loan loan) {
        BigDecimal totalAmount = calculateTotalLoanAmount(loan.getLoanAmount(), loan.getInterestRate());
        
        return CreateLoanResponse.builder()
                .id(loan.getId())
                .customerId(loan.getCustomer().getId())
                .customerName(loan.getCustomer().getName())
                .customerSurname(loan.getCustomer().getSurname())
                .loanAmount(loan.getLoanAmount())
                .interestRate(loan.getInterestRate())
                .numberOfInstallments(loan.getNumberOfInstallments())
                .createDate(loan.getCreateDate())
                .isPaid(loan.getIsPaid())
                .totalAmount(totalAmount)
                .build();
    }

    // Converts a Loan entity to LoanListResponse DTO
    public static LoanListResponse toLoanListResponse(Loan loan) {
        BigDecimal totalAmount = calculateTotalLoanAmount(loan.getLoanAmount(), loan.getInterestRate());
        int remainingInstallments = calculateRemainingInstallments(loan.getInstallments());

        return LoanListResponse.builder()
                .id(loan.getId())
                .customerId(loan.getCustomer().getId())
                .customerName(loan.getCustomer().getName())
                .customerSurname(loan.getCustomer().getSurname())
                .loanAmount(loan.getLoanAmount())
                .interestRate(loan.getInterestRate())
                .numberOfInstallments(loan.getNumberOfInstallments())
                .createDate(loan.getCreateDate())
                .isPaid(loan.getIsPaid())
                .totalAmount(totalAmount)
                .remainingInstallments(remainingInstallments)
                .build();
    }

    // Converts a LoanInstallment entity to LoanInstallmentResponse DTO
    public static LoanInstallmentResponse toLoanInstallmentResponse(LoanInstallment installment, int installmentNumber) {
        return LoanInstallmentResponse.builder()
                .id(installment.getId())
                .loanId(installment.getLoan().getId())
                .amount(installment.getAmount())
                .dueDate(installment.getDueDate())
                .paymentDate(installment.getPaymentDate())
                .paidAmount(installment.getPaidAmount())
                .isPaid(installment.getIsPaid())
                .installmentNumber(installmentNumber)
                .build();
    }

    // Converts a list of LoanInstallment entities to LoanInstallmentResponse DTOsn list of LoanInstallmentResponse DTOs
    public static List<LoanInstallmentResponse> toLoanInstallmentResponseList(List<LoanInstallment> installments) {
        return installments.stream()
                .map(installment -> {
                    int installmentNumber = installments.indexOf(installment) + 1;
                    return toLoanInstallmentResponse(installment, installmentNumber);
                })
                .collect(Collectors.toList());
    }


    // Calculates the total loan amount including interest
    public static BigDecimal calculateTotalLoanAmount(BigDecimal loanAmount, BigDecimal interestRate) {
        return loanAmount
                .multiply(BigDecimal.ONE.add(interestRate))
                .setScale(2, RoundingMode.HALF_UP);
    }

    
    // Calculates the installment amount based on total loan amount and number of installments
    public static BigDecimal calculateInstallmentAmount(BigDecimal totalAmount, int numberOfInstallments) {
        return totalAmount
                .divide(BigDecimal.valueOf(numberOfInstallments), 2, RoundingMode.HALF_UP);
    }

    
    // Calculates the number of remaining (unpaid) installments
    public static int calculateRemainingInstallments(List<LoanInstallment> installments) {
        return (int) installments.stream()
                .filter(installment -> !installment.getIsPaid())
                .count();
    }

    public static boolean isLoanFullyPaid(List<LoanInstallment> installments) {
        return installments.stream()
                .allMatch(LoanInstallment::getIsPaid);
    }
} 
package com.applab.loan_management.service;

import com.applab.loan_management.dto.CreateLoanRequest;
import com.applab.loan_management.dto.LoanListResponse;
import com.applab.loan_management.entity.Customer;
import com.applab.loan_management.entity.Loan;
import com.applab.loan_management.entity.LoanInstallment;
import com.applab.loan_management.repository.CustomerRepository;
import com.applab.loan_management.repository.LoanRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public Loan createLoan(CreateLoanRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        // Check if customer has credit fields (only CUSTOMER role should have loans)
        if (customer.getCreditLimit() == null || customer.getUsedCreditLimit() == null) {
            throw new IllegalStateException("Only customers with credit limits can create loans");
        }

        // Calculate total loan amount with interest
        BigDecimal totalAmount = request.getAmount()
                .multiply(BigDecimal.ONE.add(request.getInterestRate()))
                .setScale(2, RoundingMode.HALF_UP);

        // Check if customer has enough credit limit
        BigDecimal availableCredit = customer.getCreditLimit().subtract(customer.getUsedCreditLimit());

        if (availableCredit.compareTo(totalAmount) < 0) {
            throw new IllegalStateException("Customer does not have enough credit limit");
        }

        // Create loan
        Loan loan = Loan.builder()
                .customer(customer)
                .loanAmount(request.getAmount())
                .interestRate(request.getInterestRate())
                .numberOfInstallments(request.getNumberOfInstallments())
                .createDate(LocalDateTime.now())
                .isPaid(false)
                .build();

        // Calculate installment amount
        BigDecimal installmentAmount = totalAmount
                .divide(BigDecimal.valueOf(request.getNumberOfInstallments()), 2, RoundingMode.HALF_UP);

        // Create installments
        List<LoanInstallment> installments = new ArrayList<>();
        LocalDate firstDueDate = LocalDate.now().plusMonths(1).withDayOfMonth(1);

        for (int i = 0; i < request.getNumberOfInstallments(); i++) {
            LoanInstallment installment = LoanInstallment.builder()
                    .loan(loan)
                    .amount(installmentAmount)
                    .dueDate(firstDueDate.plusMonths(i))
                    .isPaid(false)
                    .build();
            installments.add(installment);
        }

        loan.setInstallments(installments);

        // Update customer's used credit limit
        customer.setUsedCreditLimit(customer.getUsedCreditLimit().add(totalAmount));
        customerRepository.save(customer);

        return loanRepository.save(loan);
    }

    public List<LoanListResponse> listLoans(Long customerId, Boolean isPaid, Integer numberOfInstallments) {
        // Verify customer exists
        customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found"));

        List<Loan> loans;

        // Apply filters based on provided parameters
        if (isPaid != null && numberOfInstallments != null) {
            loans = loanRepository.findByCustomerIdAndIsPaidAndNumberOfInstallments(customerId, isPaid, numberOfInstallments);
        } else if (isPaid != null) {
            loans = loanRepository.findByCustomerIdAndIsPaid(customerId, isPaid);
        } else if (numberOfInstallments != null) {
            loans = loanRepository.findByCustomerIdAndNumberOfInstallments(customerId, numberOfInstallments);
        } else {
            loans = loanRepository.findByCustomerId(customerId);
        }

        // Convert to response DTOs
        return loans.stream()
                .map(this::convertToLoanListResponse)
                .collect(Collectors.toList());
    }

    private LoanListResponse convertToLoanListResponse(Loan loan) {
        // Calculate total amount
        BigDecimal totalAmount = loan.getLoanAmount()
                .multiply(BigDecimal.ONE.add(loan.getInterestRate()))
                .setScale(2, RoundingMode.HALF_UP);

        // Calculate remaining installments
        int remainingInstallments = (int) loan.getInstallments().stream()
                .filter(installment -> !installment.getIsPaid())
                .count();

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
} 
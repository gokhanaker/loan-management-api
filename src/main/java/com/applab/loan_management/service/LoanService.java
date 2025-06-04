package com.applab.loan_management.service;

import com.applab.loan_management.dto.CreateLoanRequest;
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
} 
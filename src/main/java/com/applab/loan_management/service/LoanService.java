package com.applab.loan_management.service;

import com.applab.loan_management.dto.CreateLoanRequest;
import com.applab.loan_management.dto.LoanListResponse;
import com.applab.loan_management.dto.LoanInstallmentResponse;
import com.applab.loan_management.entity.Customer;
import com.applab.loan_management.entity.Loan;
import com.applab.loan_management.entity.LoanInstallment;
import com.applab.loan_management.exception.AdminCannotCreateLoanException;
import com.applab.loan_management.exception.CustomerNotFoundException;
import com.applab.loan_management.exception.InsufficientCreditLimitException;
import com.applab.loan_management.exception.InvalidParameterException;
import com.applab.loan_management.exception.LoanNotFoundException;
import com.applab.loan_management.exception.UnauthorizedAccessException;
import com.applab.loan_management.exception.LoanDataAccessException;
import com.applab.loan_management.repository.CustomerRepository;
import com.applab.loan_management.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
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
                .orElseThrow(() -> new CustomerNotFoundException(request.getCustomerId()));

        // Check if customer has credit fields (only CUSTOMER role should have loans)
        if (customer.getCreditLimit() == null || customer.getUsedCreditLimit() == null) {
            throw new AdminCannotCreateLoanException();
        }

        // Calculate total loan amount with interest
        BigDecimal totalAmount = request.getAmount()
                .multiply(BigDecimal.ONE.add(request.getInterestRate()))
                .setScale(2, RoundingMode.HALF_UP);

        // Check if customer has enough credit limit
        BigDecimal availableCredit = customer.getCreditLimit().subtract(customer.getUsedCreditLimit());

        if (availableCredit.compareTo(totalAmount) < 0) {
            throw new InsufficientCreditLimitException(availableCredit, totalAmount);
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
        // Validate customerId parameter
        if (customerId == null || customerId <= 0) {
            throw new InvalidParameterException("customerId", "must be a positive number");
        }

        // Validate numberOfInstallments parameter if provided
        if (numberOfInstallments != null) {
            if (numberOfInstallments <= 0) {
                throw new InvalidParameterException("numberOfInstallments", "must be a positive number");
            }
            // Check if it's one of the allowed values (6, 9, 12, 24)
            if (numberOfInstallments != 6 && numberOfInstallments != 9 && 
                numberOfInstallments != 12 && numberOfInstallments != 24) {
                throw new InvalidParameterException("numberOfInstallments", 
                    "must be one of: 6, 9, 12, or 24");
            }
        }

        // Verify customer exists
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        List<Loan> loans;

        try {
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
        } catch (Exception ex) {
            // Log the exception and rethrow as a more user-friendly message
            throw new RuntimeException("Failed to retrieve loans for customer ID: " + customerId, ex);
        }

        // Convert to response DTOs - return empty list if no loans found
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

    public List<LoanInstallmentResponse> listLoanInstallments(Long loanId, Boolean isPaid, Long customerId) {
        // Validate loanId parameter
        if (loanId == null || loanId <= 0) {
            throw new InvalidParameterException("loanId", "must be a positive number");
        }

        // Validate customerId parameter if provided (for authorization)
        if (customerId != null && customerId <= 0) {
            throw new InvalidParameterException("customerId", "must be a positive number");
        }

        try {
            // Verify loan exists and fetch with installments
            Loan loan = loanRepository.findById(loanId)
                    .orElseThrow(() -> new LoanNotFoundException(loanId));

            // Authorization check: if customerId is provided, verify the loan belongs to that customer
            if (customerId != null && !loan.getCustomer().getId().equals(customerId)) {
                throw new UnauthorizedAccessException(customerId, loanId);
            }

            // Verify loan has installments (data integrity check)
            if (loan.getInstallments() == null) {
                throw new LoanDataAccessException("Loan installments data is corrupted for loan ID: " + loanId);
            }

            List<LoanInstallment> installments = loan.getInstallments();

            // Check if installments list is empty (new loan scenario)
            if (installments.isEmpty()) {
                throw new LoanDataAccessException("No installments found for loan ID: " + loanId + ". This might indicate a data integrity issue.");
            }

            // Apply isPaid filter if provided
            if (isPaid != null) {
                installments = installments.stream()
                        .filter(installment -> {
                            if (installment.getIsPaid() == null) {
                                throw new LoanDataAccessException("Installment payment status is null for loan ID: " + loanId);
                            }
                            return installment.getIsPaid().equals(isPaid);
                        })
                        .collect(Collectors.toList());
            }

            // Convert to response DTOs with installment numbers
            List<LoanInstallmentResponse> responses = new ArrayList<>();
            
            // Get original installments list for proper numbering
            List<LoanInstallment> originalInstallments = loan.getInstallments();
            
            for (LoanInstallment installment : installments) {
                try {
                    // Find the position of this installment in the original list for proper numbering
                    int installmentNumber = originalInstallments.indexOf(installment) + 1;
                    LoanInstallmentResponse response = convertToLoanInstallmentResponse(installment, installmentNumber);
                    responses.add(response);
                } catch (Exception ex) {
                    throw new LoanDataAccessException("Failed to process installment data for loan ID: " + loanId, ex);
                }
            }

            return responses;

        } catch (LoanNotFoundException | InvalidParameterException | LoanDataAccessException ex) {
            // Re-throw our custom exceptions as-is
            throw ex;
        } catch (DataAccessException ex) {
            // Handle Spring Data Access exceptions
            throw new LoanDataAccessException("Database error while retrieving loan installments for loan ID: " + loanId, ex);
        } catch (Exception ex) {
            // Handle any other unexpected exceptions
            throw new LoanDataAccessException("Unexpected error while retrieving loan installments for loan ID: " + loanId, ex);
        }
    }

    private LoanInstallmentResponse convertToLoanInstallmentResponse(LoanInstallment installment, int installmentNumber) {
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
} 
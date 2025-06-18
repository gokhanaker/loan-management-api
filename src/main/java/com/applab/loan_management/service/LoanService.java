package com.applab.loan_management.service;

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
import com.applab.loan_management.exception.LoanNotFoundException;
import com.applab.loan_management.exception.LoanDataAccessException;
import com.applab.loan_management.exception.LoanAlreadyPaidException;
import com.applab.loan_management.exception.InvalidPaymentAmountException;
import com.applab.loan_management.exception.NoPayableInstallmentsException;
import com.applab.loan_management.exception.CustomerAccessDeniedException;
import com.applab.loan_management.repository.CustomerRepository;
import com.applab.loan_management.repository.LoanRepository;
import com.applab.loan_management.util.LoanMapperUtil;
import com.applab.loan_management.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final CurrentUserService currentUserService;

    @Transactional
    public Loan createLoan(CreateLoanRequest request) {
        // Authorization check: ensure current user can access this customer's data
        if (!currentUserService.canAccessCustomerData(request.getCustomerId())) {
            Long currentCustomerId = currentUserService.getCurrentCustomerId();
            throw new CustomerAccessDeniedException(request.getCustomerId(), currentCustomerId);
        }

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException(request.getCustomerId()));

        // Check if customer has credit fields (only CUSTOMER role should have loans)
        if (customer.getCreditLimit() == null || customer.getUsedCreditLimit() == null) {
            throw new AdminCannotCreateLoanException();
        }

        // Calculate total loan amount with interest using utility method
        BigDecimal totalAmount = LoanMapperUtil.calculateTotalLoanAmount(request.getAmount(), request.getInterestRate());

        BigDecimal availableCredit = customer.getCreditLimit().subtract(customer.getUsedCreditLimit());

        if (availableCredit.compareTo(totalAmount) < 0) {
            throw new InsufficientCreditLimitException(availableCredit, totalAmount);
        }

        Loan loan = Loan.builder()
                .customer(customer)
                .loanAmount(request.getAmount())
                .interestRate(request.getInterestRate())
                .numberOfInstallments(request.getNumberOfInstallments())
                .createDate(LocalDateTime.now())
                .isPaid(false)
                .build();

        // Calculate installment amount
        BigDecimal installmentAmount = LoanMapperUtil.calculateInstallmentAmount(totalAmount, request.getNumberOfInstallments());

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
        if (customerId == null || customerId <= 0) {
            throw new InvalidParameterException("customerId", "must be a positive number");
        }

        // Authorization check: ensure current user can access this customer's data
        if (!currentUserService.canAccessCustomerData(customerId)) {
            Long currentCustomerId = currentUserService.getCurrentCustomerId();
            throw new CustomerAccessDeniedException(customerId, currentCustomerId);
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

        customerRepository.findById(customerId)
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
            throw new RuntimeException("Failed to retrieve loans for customer ID: " + customerId, ex);
        }

        return loans.stream()
                .map(LoanMapperUtil::toLoanListResponse)
                .collect(Collectors.toList());
    }

    public List<LoanInstallmentResponse> listLoanInstallments(Long loanId) {
        if (loanId == null || loanId <= 0) {
            throw new InvalidParameterException("loanId", "must be a positive number");
        }

        try {
            Loan loan = loanRepository.findById(loanId)
                    .orElseThrow(() -> new LoanNotFoundException(loanId));

            // Authorization check: ensure current user can access this loan's customer data
            Long loanCustomerId = loan.getCustomer().getId();
            if (!currentUserService.canAccessCustomerData(loanCustomerId)) {
                Long currentCustomerId = currentUserService.getCurrentCustomerId();
                throw new CustomerAccessDeniedException(loanCustomerId, currentCustomerId);
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

            return LoanMapperUtil.toLoanInstallmentResponseList(installments);

        } catch (LoanNotFoundException | InvalidParameterException | LoanDataAccessException | CustomerAccessDeniedException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            throw new LoanDataAccessException("Database error while retrieving loan installments for loan ID: " + loanId, ex);
        } catch (Exception ex) {
            throw new LoanDataAccessException("Unexpected error while retrieving loan installments for loan ID: " + loanId, ex);
        }
    }

    @Transactional
    public PayLoanResponse payLoan(Long loanId, PayLoanRequest request) {
        if (loanId == null || loanId <= 0) {
            throw new InvalidParameterException("loanId", "must be a positive number");
        }

        try {
            Loan loan = loanRepository.findById(loanId)
                    .orElseThrow(() -> new LoanNotFoundException(loanId));

            // Authorization check: ensure current user can access this loan's customer data
            Long loanCustomerId = loan.getCustomer().getId();
            if (!currentUserService.canAccessCustomerData(loanCustomerId)) {
                Long currentCustomerId = currentUserService.getCurrentCustomerId();
                throw new CustomerAccessDeniedException(loanCustomerId, currentCustomerId);
            }

            if (Boolean.TRUE.equals(loan.getIsPaid())) {
                throw new LoanAlreadyPaidException(loanId);
            }

            // Get unpaid installments sorted by due date (earliest first)
            LocalDate currentDate = LocalDate.now();
            LocalDate maxPayableDate = currentDate.plusMonths(3);

            List<LoanInstallment> payableInstallments = loan.getInstallments().stream()
                    .filter(installment -> !installment.getIsPaid())
                    .filter(installment -> !installment.getDueDate().isAfter(maxPayableDate))
                    .sorted((i1, i2) -> i1.getDueDate().compareTo(i2.getDueDate()))
                    .collect(Collectors.toList());

            if (payableInstallments.isEmpty()) {
                throw new NoPayableInstallmentsException(loanId);
            }

            // Check if payment amount can cover at least one installment
            BigDecimal remainingAmount = request.getAmount();
            BigDecimal firstInstallmentAmount = payableInstallments.get(0).getAmount();
            
            if (remainingAmount.compareTo(firstInstallmentAmount) < 0) {
                throw new InvalidPaymentAmountException(remainingAmount, firstInstallmentAmount);
            }

            // Process payments
            int installmentsPaid = 0;
            BigDecimal totalAmountSpent = BigDecimal.ZERO;
            
            for (LoanInstallment installment : payableInstallments) {
                BigDecimal installmentAmount = installment.getAmount();
                
                // Check if we have enough money to pay this installment
                if (remainingAmount.compareTo(installmentAmount) >= 0) {
                    // Pay this installment
                    installment.setPaidAmount(installmentAmount);
                    installment.setPaymentDate(currentDate);
                    installment.setIsPaid(true);
                    
                    remainingAmount = remainingAmount.subtract(installmentAmount);
                    totalAmountSpent = totalAmountSpent.add(installmentAmount);
                    installmentsPaid++;
                } else {
                    // Not enough money for this installment, stop here
                    break;
                }
            }

            // Check if all installments are now paid using utility method
            boolean isLoanFullyPaid = LoanMapperUtil.isLoanFullyPaid(loan.getInstallments());

            if (isLoanFullyPaid) {
                loan.setIsPaid(true);
            }

            Customer customer = loan.getCustomer();
            customer.setUsedCreditLimit(customer.getUsedCreditLimit().subtract(totalAmountSpent));
            customerRepository.save(customer);

            loanRepository.save(loan);

            String message = String.format("Successfully paid %d installment(s) for a total of %.2f", 
                    installmentsPaid, totalAmountSpent);
            
            if (isLoanFullyPaid) {
                message += ". Loan is now fully paid!";
            }

            return PayLoanResponse.builder()
                    .installmentsPaid(installmentsPaid)
                    .totalAmountSpent(totalAmountSpent)
                    .isLoanFullyPaid(isLoanFullyPaid)
                    .message(message)
                    .build();

        } catch (LoanNotFoundException | InvalidParameterException | LoanAlreadyPaidException | 
                 InvalidPaymentAmountException | NoPayableInstallmentsException | CustomerAccessDeniedException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            throw new LoanDataAccessException("Database error while processing loan payment for loan ID: " + loanId, ex);
        } catch (Exception ex) {
            throw new LoanDataAccessException("Unexpected error while processing loan payment for loan ID: " + loanId, ex);
        }
    }
} 
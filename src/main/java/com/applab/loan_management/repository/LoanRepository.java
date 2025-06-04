package com.applab.loan_management.repository;

import com.applab.loan_management.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    
    // Find all loans for a specific customer
    List<Loan> findByCustomerId(Long customerId);
    
    // Find loans for a customer with specific payment status
    List<Loan> findByCustomerIdAndIsPaid(Long customerId, Boolean isPaid);
    
    // Find loans for a customer with specific number of installments
    List<Loan> findByCustomerIdAndNumberOfInstallments(Long customerId, Integer numberOfInstallments);
    
    // Find loans for a customer with both payment status and number of installments
    List<Loan> findByCustomerIdAndIsPaidAndNumberOfInstallments(Long customerId, Boolean isPaid, Integer numberOfInstallments);
} 
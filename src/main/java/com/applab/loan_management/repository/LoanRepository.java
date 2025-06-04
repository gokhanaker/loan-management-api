package com.applab.loan_management.repository;

import com.applab.loan_management.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, Long> {
} 
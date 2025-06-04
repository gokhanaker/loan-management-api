package com.applab.loan_management.controller;

import com.applab.loan_management.dto.CreateLoanRequest;
import com.applab.loan_management.dto.CreateLoanResponse;
import com.applab.loan_management.entity.Loan;
import com.applab.loan_management.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @PostMapping
    public ResponseEntity<CreateLoanResponse> createLoan(@Valid @RequestBody CreateLoanRequest request) {
        Loan loan = loanService.createLoan(request);
        
        // Calculate total amount (loan amount + interest)
        BigDecimal totalAmount = request.getAmount()
                .multiply(BigDecimal.ONE.add(request.getInterestRate()));
        
        // Create response DTO without installments
        CreateLoanResponse response = CreateLoanResponse.builder()
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
        
        return ResponseEntity.ok(response);
    }
} 
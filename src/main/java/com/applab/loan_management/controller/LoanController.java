package com.applab.loan_management.controller;

import com.applab.loan_management.dto.CreateLoanRequest;
import com.applab.loan_management.dto.CreateLoanResponse;
import com.applab.loan_management.dto.LoanListResponse;
import com.applab.loan_management.dto.LoanInstallmentResponse;
import com.applab.loan_management.entity.Loan;
import com.applab.loan_management.service.LoanService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
@Validated
public class LoanController {

    private final LoanService loanService;

    @PostMapping
    public ResponseEntity<CreateLoanResponse> createLoan(@Valid @RequestBody CreateLoanRequest request) {
        Loan loan = loanService.createLoan(request);
        
        BigDecimal totalAmount = request.getAmount()
                .multiply(BigDecimal.ONE.add(request.getInterestRate()));
        
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

    @GetMapping
    public ResponseEntity<List<LoanListResponse>> listLoans(
            @RequestParam @Min(value = 1, message = "Customer ID must be a positive number") Long customerId,
            @RequestParam(required = false) Boolean isPaid,
            @RequestParam(required = false) Integer numberOfInstallments) {
        
        List<LoanListResponse> loans = loanService.listLoans(customerId, isPaid, numberOfInstallments);
        return ResponseEntity.ok(loans);
    }

    @GetMapping("/{loanId}/installments")
    public ResponseEntity<List<LoanInstallmentResponse>> listLoanInstallments(
            @PathVariable @Min(value = 1, message = "Loan ID must be a positive number") Long loanId) {
        
        List<LoanInstallmentResponse> installments = loanService.listLoanInstallments(loanId);
        return ResponseEntity.ok(installments);
    }
} 
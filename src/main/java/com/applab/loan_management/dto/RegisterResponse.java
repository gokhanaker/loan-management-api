package com.applab.loan_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterResponse {
    private String token;
    private String message;
    private Long userId;
    private UserProfile user;
    private LocalDateTime registrationTime;
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserProfile {
        private String name;
        private String surname;
        private String email;
        private String role;
    }
} 
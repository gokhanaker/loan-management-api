# 🏦 Loan Management System

A Spring Boot REST API for managing bank loans with JWT authentication, role-based authorization, and complete loan lifecycle management.

## 🚀 Overview

This project implements a complete loan management system for a bank, allowing customers to apply for loans and make payments while providing administrators with full oversight capabilities.

## ✨ Features

### 🔐 Authentication & Authorization

- **JWT-based authentication** with role-based access control
- **Customer role**: Can only access own loan data
- **Admin role**: Can access all customer data without restrictions

### 💰 Loan Management

- **Create loans** with validation (amount, interest rate, installments)
- **List loans** with optional filters (payment status, installment count)
- **View installments** for specific loans
- **Pay installments** with intelligent payment distribution

## 🛠️ Technology Stack

- **Java 21**
- **Spring Boot 3.5.0**

## 🚀 Quick Start

### Prerequisites

- Java 21 or higher
- Maven 3.6+

### Installation & Running

```bash
# Clone the repository
git clone https://github.com/gokhanaker/loan-management-api
cd loan-management-api

# Build the project
./mvnw clean compile

# Run the application
./mvnw spring-boot:run

# Execute unit tests
./mvnw test
```

The application will start on `http://localhost:8080`

### Database Access (Development)

H2 in memory database is used for development purpose. For this reason data records in db will disappear when app stops

- **H2 Console**: http://localhost:8080/h2-console
- **JDBC URL**: `jdbc:h2:mem:testdb`
- **Username**: `sa`
- **Password**: (leave empty)

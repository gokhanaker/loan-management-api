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
```

The application will start on `http://localhost:8080`

### Database Access (Development)
H2 in memory database is used for development purpose. For this reason data records in db will disappear when app stops

- **H2 Console**: http://localhost:8080/h2-console
- **JDBC URL**: `jdbc:h2:mem:testdb`
- **Username**: `sa`
- **Password**: (leave empty)

## 📚 API Documentation

### Base URL
```
http://localhost:8080
```

### Authentication Endpoints (Public)

#### Register Customer
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "customer@example.com",
  "password": "password123",
  "role": "CUSTOMER",
  "name": "John",
  "surname": "Doe",
  "creditLimit": 10000.00,
  "usedCreditLimit": 0.00
}
```

#### Register Admin
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "admin@bank.com",
  "password": "admin123",
  "role": "ADMIN",
  "name": "Bank",
  "surname": "Admin"
}
```

#### Login
```http
POST /api/auth/authenticate
Content-Type: application/json

{
  "email": "customer@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### Loan Endpoints (Protected - Require JWT)

#### Create Loan
```http
POST /api/loans
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "customerId": 1,
  "amount": 5000.00,
  "interestRate": 0.2,
  "numberOfInstallments": 12
}
```

#### List Loans
```http
GET /api/loans?customerId=1&isPaid=false&numberOfInstallments=12
Authorization: Bearer <jwt-token>
```

#### List Loan Installments
```http
GET /api/loans/1/installments
Authorization: Bearer <jwt-token>
```

#### Pay Loan
```http
POST /api/loans/1/pay
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "amount": 5000
}
```
#### API Testing

Please check the  ```Loan_Management_API.postman_collection.json``` file in the repo. It is a postman collection of this project

## 🔒 Security Implementation

### JWT Authentication
- **Token Generation**: Contains user email, role, and customer ID
- **Token Validation**: Verified on every protected endpoint
- **Token Storage**: Stored in Authorization header as Bearer token

### Role-Based Authorization
- **CUSTOMER Role**: 
  - Can only access loans where `customerId` matches their JWT token
  - Cannot access other customers' data
  - Returns `403 Forbidden` for unauthorized access

- **ADMIN Role**:
  - Can access any customer's loan data
  - No restrictions on `customerId` parameters
  - Full administrative privileges

### Security Headers
```http
Authorization: Bearer <jwt-token>
Content-Type: application/json
```

## 🗄️ Database Schema

### Customer Table
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT (PK) | Unique customer identifier |
| email | VARCHAR(255) | Customer email (unique) |
| password | VARCHAR(255) | BCrypt hashed password |
| role | VARCHAR(50) | CUSTOMER or ADMIN |
| name | VARCHAR(100) | First name |
| surname | VARCHAR(100) | Last name |
| credit_limit | DECIMAL(19,2) | Maximum credit limit |
| used_credit_limit | DECIMAL(19,2) | Currently used credit |

### Loan Table
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT (PK) | Unique loan identifier |
| customer_id | BIGINT (FK) | Reference to customer |
| loan_amount | DECIMAL(19,2) | Original loan amount |
| interest_rate | DECIMAL(5,3) | Interest rate (0.1-0.5) |
| number_of_installments | INTEGER | 6, 9, 12, or 24 |
| create_date | TIMESTAMP | Loan creation date |
| is_paid | BOOLEAN | Loan completion status |

### Loan Installment Table
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT (PK) | Unique installment identifier |
| loan_id | BIGINT (FK) | Reference to loan |
| amount | DECIMAL(19,2) | Installment amount |
| paid_amount | DECIMAL(19,2) | Amount actually paid |
| due_date | DATE | Payment due date |
| payment_date | DATE | Actual payment date |
| is_paid | BOOLEAN | Payment status |


## 🏆 Key Implementation Highlights

1. **Customer Role**: Added role field to Customer entity to represent if user is a CUSTOMER or ADMIN. Admin users don't have credit_limit or used_credit_limit fields and authorized to all loan operations
2. **Security First**: JWT authentication with role-based authorization. The Loan endpoints are protected and require a valid JWT
3. **Data Isolation**: Customers can only access their own data and can not access to loans that belong to another customer

# Payment System

A full-stack payment system built as a learning project to practice Spring Boot 3, JWT authentication, REST API design, and React. Simulates core banking operations: user registration, account management, deposits, and fund transfers.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 4.0, Spring Security, Spring Data JPA |
| Authentication | JWT (JJWT 0.12.3) |
| Database | PostgreSQL 15+ |
| ORM | Hibernate (via Spring Data JPA) |
| Validation | Jakarta Bean Validation |
| Boilerplate reduction | Lombok |
| Frontend | React 19, Vite, React Router v7, Axios |
| Test (backend) | JUnit 5, Mockito, MockMvc, H2 (in-memory) |

---

## Project Structure

```
payment-system/
├── backend/                         # Spring Boot REST API
│   └── src/main/java/com/payment/backend/
│       ├── config/                  # Security & app configuration
│       ├── controller/              # REST endpoints (HTTP layer only)
│       ├── dto/                     # Request / Response DTOs
│       ├── entity/                  # JPA entities (User, Account, Transaction)
│       ├── exception/               # Custom exceptions + global handler
│       ├── repository/              # Spring Data JPA repositories
│       ├── security/                # JWT filter, JwtService, UserDetailsService
│       └── service/                 # Business logic layer
│
├── frontend/                        # React SPA
│   └── src/
│       ├── components/              # AccountCard, DepositForm, TransferForm
│       ├── context/                 # AuthContext (JWT storage + state)
│       ├── pages/                   # LoginPage, RegisterPage, DashboardPage
│       └── components/axiosClient.js # Axios instance with auth header
│
└── files/
    ├── payment_system_schema.sql    # Full production-grade DB schema
    └── data_dictionary.md           # Schema documentation with interview notes
```

---

## Architecture

This project follows a strict layered architecture:

```
HTTP Request
     ↓
@RestController   ← Parse request → call service → return ResponseDTO
     ↓
@Service          ← All business logic lives here. @Transactional here only.
     ↓
@Repository       ← Data access only. Returns Optional<T> for nullable results.
     ↓
PostgreSQL
```

**Key design decisions:**
- Controllers never call repositories directly — service layer is mandatory
- Entities are never returned from REST endpoints — always mapped to DTOs first
- Constructor injection via `@RequiredArgsConstructor` (never `@Autowired` on fields)
- Custom exceptions (`ResourceNotFoundException`, `InsufficientFundsException`, `ConflictException`) mapped to HTTP status codes by `GlobalExceptionHandler`

---

## API Endpoints

### Auth — `/api/auth`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/login` | No | Authenticate user, returns JWT token |

**Login request body:**
```json
{
  "email": "huy@example.com",
  "password": "secret123"
}
```

**Login response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": 1,
  "firstName": "Huy"
}
```

---

### Users — `/api/users`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/users/register` | No | Register a new user |
| `GET` | `/api/users/{id}` | Yes | Get user details by ID |

**Register request body:**
```json
{
  "firstName": "Huy",
  "lastName": "Van",
  "email": "huy@example.com",
  "password": "secret123"
}
```

---

### Accounts — `/api/accounts`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/accounts/user/{userId}` | Yes | Create a new account for a user |
| `GET` | `/api/accounts/user/{userId}` | Yes | Get all accounts for a user |
| `GET` | `/api/accounts/{accountNumber}` | Yes | Get account by account number |
| `POST` | `/api/accounts/{accountNumber}/deposit` | Yes | Deposit funds into account |
| `POST` | `/api/accounts/transfer` | Yes | Transfer funds between accounts |

**Deposit request body:**
```json
{ "amount": "500.00" }
```

**Transfer request body:**
```json
{
  "senderAccountNumber": "ACC-1001",
  "receiverAccountNumber": "ACC-1002",
  "amount": "250.00"
}
```

> **Note:** Authenticated endpoints require an `Authorization: Bearer <token>` header.

---

## Database Schema

The application uses three JPA entities backed by PostgreSQL tables:

| Entity | Table | Description |
|---|---|---|
| `User` | `users` | Account holder (email, hashed password, name) |
| `Account` | `accounts` | Payment account with balance — one user can have many |
| `Transaction` | `transactions` | Immutable record of every deposit and transfer |

The `files/` directory also contains a **production-grade extended schema** (`payment_system_schema.sql`) with:
- `subscribers`, `accounts`, `payees`, `payments`, `audit_log` tables
- Soft deletes (`deleted_at` column), status lifecycle columns
- Performance indexes on FK and filter columns
- Views for dashboard and recent-payment queries

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 15+ running locally
- Node.js 18+ and npm

---

### Backend Setup

**1. Create the database:**

```sql
CREATE DATABASE payment_system;
```

**2. Configure `application.properties`:**

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/payment_system
spring.datasource.username=postgres
spring.datasource.password=your_password

spring.jpa.hibernate.ddl-auto=update

jwt.secret=your_256bit_hex_secret
jwt.expiration=86400000
```

> ⚠️ Do not commit real credentials. Use environment variables in any non-local environment:
> ```properties
> spring.datasource.password=${DB_PASSWORD}
> jwt.secret=${JWT_SECRET}
> ```

**3. Run the backend:**

```bash
cd backend
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`.

---

### Frontend Setup

```bash
cd frontend
npm install
npm run dev
```

The React app starts on `http://localhost:5173` and proxies API calls to `http://localhost:8080`.

---

### Running Tests

```bash
cd backend
./mvnw test
```

The test suite uses an H2 in-memory database — no PostgreSQL required to run tests.

Test coverage includes:
- `AccountServiceTest` — transfer logic, insufficient funds, deposit
- `UserServiceTest` — registration, duplicate email conflict
- `AccountControllerTest` — MockMvc integration tests for all account endpoints
- `UserControllerTest` — MockMvc integration tests for register and get
- `JwtServiceTest` — token generation and validation

---

## Security

- Passwords are hashed with **BCrypt** before storage — never stored in plaintext
- All `/api/accounts/**` and `/api/users/**` endpoints (except `/register`) require a valid **JWT Bearer token**
- JWT tokens expire after **24 hours** (configurable via `jwt.expiration`)
- Token validation is handled by `JwtAuthenticationFilter` before the request reaches any controller

---

## What I Learned Building This

> This is a learning project — documented here for interview readiness.

| Topic | What I practiced |
|---|---|
| Spring Security | Stateless JWT filter chain, `SecurityConfig`, `UserDetailsService` |
| REST API design | HTTP verbs, status codes (201 Created, 404 Not Found, 409 Conflict) |
| Layered architecture | Strict separation: Controller → Service → Repository |
| DTOs | Decoupling API contract from DB schema — `User` entity never exposed directly |
| Exception handling | `@RestControllerAdvice` global handler; domain exceptions map to HTTP codes |
| JPA | `@OneToMany` relationships, `Optional<T>` returns, `@Transactional` on service layer |
| Testing | MockMvc for controller tests, Mockito for service layer, H2 for integration tests |
| React + JWT | `AuthContext` for token state, Axios interceptors for `Authorization` header |

---

## Known Limitations / Future Improvements

- [ ] Role-based access control (admin vs. customer roles)
- [ ] Account number ownership validation on transfer (prevent spoofed `senderAccountNumber`)
- [ ] Pagination on transaction history endpoints
- [ ] Idempotency keys on transfer endpoint (prevent duplicate submissions)
- [ ] Flyway migrations instead of `ddl-auto=update`
- [ ] Docker Compose setup for one-command local startup
- [ ] Scheduled payment support (from extended schema)

---

## Author

**Huy Van** — Software Engineer  
Built as part of a 12-week full-stack learning plan covering Spring Boot 3, REST APIs, and React.

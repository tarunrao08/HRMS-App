# HRMS Backend

Human Resource Management System — REST API built with Spring Boot 3.2.5 and Java 21.

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 3.2.5 |
| Security | Spring Security 6 + JWT (jjwt 0.12.6) |
| Database | PostgreSQL |
| ORM | Spring Data JPA + Hibernate |
| Migrations | Flyway 10 |
| Scheduler | Quartz |
| Mapping | MapStruct 1.5.5 |
| PDF Generation | iText 8 (AGPL) |
| API Docs | SpringDoc OpenAPI 2.5 (Swagger UI) |
| Build | Maven |

## Modules

| Package | Description |
|---|---|
| `auth` | JWT authentication, login, token refresh, user management |
| `employee` | Employee profiles, departments, designations, branches |
| `attendance` | Punch-in/out, shifts, monthly summaries |
| `leave` | Leave types, balances, requests, multi-level approvals |
| `payroll` | Salary structures (CTC), payroll runs, payslips, PDF generation |
| `onboarding` | Onboarding workflows and task checklists |
| `bot` | Rule-based HR assistant chatbot (role-aware) |
| `common` | Shared DTOs, exceptions, base entities, API response wrapper |

## Prerequisites

- Java 21+
- PostgreSQL 14+
- Maven 3.9+ (or use the wrapper inside IntelliJ)

## Database Setup

```sql
CREATE DATABASE hrms_db;
```

Flyway runs all migrations automatically on startup. Migration files are in `src/main/resources/db/migration/`:

| File | Description |
|---|---|
| V1 | Employees, departments, designations, branches |
| V2 | Users, roles |
| V3 | Attendance records and monthly summaries |
| V4 | Leave types, balances, requests, approvals |
| V5 | Salary structures, payroll runs, payslips |
| V6 | Onboarding workflows and tasks |
| V7–V10 | Schema fixes and refinements |

## Configuration

All settings live in `src/main/resources/application.yml`. Key values to change for your environment:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/hrms_db
    username: postgres
    password: postgres

app:
  jwt:
    secret: <your-256-bit-hex-secret>
    expiration: 86400000       # 24 hours
    refresh-expiration: 604800000  # 7 days

  upload-dir: uploads
  payroll:
    pdf-dir: ${user.home}/hrms-payslips

  cors:
    allowed-origins:
      - http://localhost:5173
      - http://localhost:3000
```

> **JWT Secret**: generate a secure 256-bit hex string. The default in the repo is for development only — replace it in production.

## Running Locally

```bash
# From IntelliJ or any terminal with Maven available:
mvn spring-boot:run
```

The server starts on **http://localhost:8080**.

## API Documentation

Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

## Roles

| Role | Access |
|---|---|
| `ROLE_HR_ADMIN` | Full access — all employees, payroll, leave admin, user accounts |
| `ROLE_MANAGER` | Employees under them, leave approvals, payroll generation |
| `ROLE_EMPLOYEE` | Own profile, attendance, leave, payslips |

## Key API Endpoints

### Auth
| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/login` | Login and receive JWT |
| POST | `/api/auth/refresh` | Refresh access token |
| POST | `/api/auth/logout` | Invalidate token |

### Employees
| Method | Path | Description |
|---|---|---|
| GET | `/api/employees` | List all employees (paginated) |
| POST | `/api/employees` | Create employee |
| GET | `/api/employees/{id}` | Get employee by ID |
| PUT | `/api/employees/{id}` | Update employee |

### Payroll
| Method | Path | Description |
|---|---|---|
| GET | `/api/payroll/salary-structures` | List salary structures |
| POST | `/api/payroll/salary-structures` | Assign CTC to employee |
| GET | `/api/payroll/salary-structures/my` | My active salary structure |
| GET | `/api/payroll/salary-structures/summary` | Total monthly payroll (HR Admin) |
| POST | `/api/payroll/payslips/generate` | Generate payslip for an employee |
| GET | `/api/payroll/payslips/me` | My payslips |
| GET | `/api/payroll/payslips/{id}/pdf` | Download payslip PDF |

### Leave
| Method | Path | Description |
|---|---|---|
| GET | `/api/leave/types` | List leave types |
| GET | `/api/leave/balances/me` | My leave balances |
| POST | `/api/leave/requests` | Apply for leave |
| GET | `/api/leave/requests` | List all leave requests (HR Admin) |
| PUT | `/api/leave/requests/{id}/approve` | Approve leave request |
| PUT | `/api/leave/requests/{id}/reject` | Reject leave request |

### Bot
| Method | Path | Description |
|---|---|---|
| POST | `/api/bot/chat` | Send message to HR assistant |

## Scheduled Jobs

| Job | Schedule | Description |
|---|---|---|
| Leave Rollover | Jan 1 at 00:05 | Carries forward unused leave balances to the new year |

## Project Structure

```
src/main/java/com/hrms/
├── auth/           # JWT filter, auth controller, user entity
├── employee/       # Employee, Department, Designation, Branch
├── attendance/     # AttendanceRecord, EmployeeShift, MonthlySummary
├── leave/          # LeaveType, LeaveBalance, LeaveRequest, LeaveApproval
├── payroll/        # SalaryStructure, PayrollRun, Payslip, PDF service
├── onboarding/     # OnboardingWorkflow, OnboardingTask
├── bot/            # BotController, BotService (rule-based chatbot)
└── common/         # ApiResponse, BaseEntity, exceptions, pagination
```

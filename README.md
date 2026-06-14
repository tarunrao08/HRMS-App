# HRMS App

A full-stack Human Resource Management System built with **Spring Boot** (backend) and **React** (frontend).

## Repository Structure

```
HRMS-App/
├── backend/    Spring Boot 3.2.5 REST API (Java 21)
└── frontend/   React 18 + TypeScript web application
```

Each sub-project has its own README with detailed setup instructions:

- [backend/README.md](backend/README.md)
- [frontend/README.md](frontend/README.md)

## Features

| Module | Description |
|---|---|
| Authentication | JWT-based login, token refresh, role-based access control |
| Employees | Employee profiles, departments, designations, branches |
| Attendance | Punch-in/out, shift management, monthly summaries |
| Leave | Leave applications, multi-level approvals, balance tracking |
| Payroll | CTC assignment, payslip generation, PDF download |
| Onboarding | Workflow templates, task checklists for new joiners |
| HR Assistant | Rule-based chatbot for employee and HR admin queries |

## Tech Stack

| | Backend | Frontend |
|---|---|---|
| Language | Java 21 | TypeScript 5.6 |
| Framework | Spring Boot 3.2.5 | React 18 + Vite 5 |
| Database | PostgreSQL + Flyway | — |
| Cache | Redis | — |
| Auth | Spring Security 6 + JWT | Zustand |
| UI | — | Radix UI + Tailwind CSS |
| API Docs | Swagger UI | — |

## Roles

| Role | Access |
|---|---|
| `ROLE_HR_ADMIN` | Full access — all modules |
| `ROLE_MANAGER` | Employees, payroll, leave approvals |
| `ROLE_EMPLOYEE` | Own dashboard, attendance, leave, payslips |

## Quick Start

### Prerequisites

- Java 21+
- Node.js 18+
- PostgreSQL 14+ (database: `hrms_db`)
- Redis 7+

### 1. Start the backend

```bash
cd backend
# Update src/main/resources/application.yml with your DB credentials
mvn spring-boot:run
# Runs on http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

### 2. Start the frontend

```bash
cd frontend
npm install
npm run dev
# Runs on http://localhost:5173
# API calls to /api are proxied to http://localhost:8080
```

Open **http://localhost:5173** in your browser.

## Screenshots

> Coming soon

## License

This project is for educational and personal use.

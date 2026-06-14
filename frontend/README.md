# HRMS Frontend

Human Resource Management System — web application built with React 18, TypeScript, and Vite.

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | React 18 |
| Language | TypeScript 5.6 |
| Build Tool | Vite 5 |
| Routing | React Router DOM 6 |
| State Management | Zustand 5 |
| HTTP Client | Axios |
| UI Components | Radix UI primitives + custom components |
| Styling | Tailwind CSS 3 |
| Icons | Lucide React |
| Notifications | React Hot Toast |
| Testing | Playwright |

## Prerequisites

- Node.js 18+
- npm or yarn
- HRMS Backend running on `http://localhost:8080`

## Getting Started

```bash
# Install dependencies
npm install

# Start development server
npm run dev
```

The app runs on **http://localhost:5173**. API calls to `/api/*` are proxied to `http://localhost:8080`.

## Available Scripts

| Script | Description |
|---|---|
| `npm run dev` | Start development server (port 5173) |
| `npm run build` | Type-check and build for production |
| `npm run preview` | Preview production build locally |
| `npm run lint` | Run ESLint |
| `npm run test:e2e` | Run Playwright end-to-end tests |
| `npm run test:e2e:ui` | Run Playwright tests with UI |
| `npm run test:e2e:headed` | Run Playwright tests in headed browser |

## Project Structure

```
src/
├── assets/           # Static assets (images, fonts)
├── components/
│   ├── shared/       # Layout, Sidebar, Header, ChatBot, ProtectedRoute
│   └── ui/           # Reusable UI primitives (Button, Card, Dialog, etc.)
├── hooks/            # Custom React hooks (useRole, etc.)
├── layouts/
│   ├── AppLayout.tsx    # Authenticated layout with Sidebar + Header
│   └── AuthLayout.tsx   # Public layout for login page
├── lib/              # Utility functions (cn, formatters)
├── pages/
│   ├── attendance/   # Punch-in/out, attendance records
│   ├── auth/         # Login page
│   ├── branches/     # Branch management (HR Admin)
│   ├── dashboard/    # Role-aware dashboard with stats
│   ├── departments/  # Department management (HR Admin)
│   ├── designations/ # Designation management (HR Admin)
│   ├── employees/    # Employee list and profiles
│   ├── errors/       # 403 Forbidden page
│   ├── leave/        # Leave requests, approvals, admin
│   ├── onboarding/   # Onboarding workflows and tasks
│   ├── payroll/      # Salary structures, payroll runs, payslips
│   └── users/        # User account management (HR Admin)
├── services/         # Axios service modules (one per API domain)
├── store/            # Zustand stores (authStore)
└── types/            # Shared TypeScript interfaces
```

## Roles and Access

| Role | Accessible Pages |
|---|---|
| `ROLE_HR_ADMIN` | All pages |
| `ROLE_MANAGER` | Dashboard, Attendance, Leave, Employees, Payroll, Approvals, Onboarding |
| `ROLE_EMPLOYEE` | Dashboard, Attendance, My Leave, Onboarding |

Access control is enforced on both the frontend (route guards via `ProtectedRoute`) and the backend (Spring Security).

## Features

### Dashboard
- **HR Admin**: Total employee count, today's attendance, pending leaves, total monthly payroll across all active employees
- **Employee**: Same stats scoped to self; latest payslip summary with PDF download; onboarding checklist if active

### Employees
- Full employee directory with search and filters
- Create, view, and update employee profiles
- Department, designation, branch, and manager assignment

### Attendance
- Punch-in / punch-out
- Monthly attendance summary
- Shift management
- HR Admin: full attendance log with filters

### Leave
- Apply for leave with date range and type selection
- View leave balances per type
- Multi-level approval workflow for managers and HR
- HR Admin: leave type configuration, all-employee leave admin

### Payroll
- HR Admin / Manager: assign CTC (salary structure) to employees
- HR Admin / Manager: generate payslip for any employee for any month/year
- Payroll runs: DRAFT → PROCESSED → APPROVED → DISBURSED
- Employee: view own salary structure and published payslips
- PDF download for generated payslips

### Onboarding
- HR Admin: create onboarding workflows and assign tasks to new employees
- Employee: view and complete their onboarding checklist

### HR Assistant (ChatBot)
A floating chat bubble (bottom-right corner) available on every page for all authenticated users.

**Employee can ask about:**
- Salary details and latest payslip breakdown
- Leave balances by leave type
- Recent leave request status
- Monthly attendance summary
- Own profile details
- Company policies (WFH, notice period, office hours, holidays)

**HR Admin can additionally ask about:**
- Total employee headcount and new joiners this month
- Monthly payroll summary across all active employees
- Count of pending leave approvals
- Attendance overview for the current month

## API Proxy

Vite proxies all `/api` requests to the backend in development:

```ts
// vite.config.ts
server: {
  proxy: {
    "/api": "http://localhost:8080"
  }
}
```

No environment variables are needed for local development.

## Building for Production

```bash
npm run build
```

Output goes to the `dist/` folder. Configure your web server (nginx, etc.) to:
1. Serve `dist/` as static files
2. Proxy `/api/*` to the backend server
3. Redirect all non-asset 404s to `index.html` for client-side routing

### Example nginx config

```nginx
server {
    listen 80;
    root /var/www/hrms/dist;
    index index.html;

    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

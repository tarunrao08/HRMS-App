import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom"
import { Toaster } from "react-hot-toast"
import AuthLayout        from "@/layouts/AuthLayout"
import AppLayout         from "@/layouts/AppLayout"
import ProtectedRoute    from "@/components/shared/ProtectedRoute"
import ChatBot           from "@/components/shared/ChatBot"
import LoginPage         from "@/pages/auth/LoginPage"
import DashboardPage     from "@/pages/dashboard/DashboardPage"
import EmployeesPage     from "@/pages/employees/EmployeesPage"
import DepartmentsPage   from "@/pages/departments/DepartmentsPage"
import DesignationsPage  from "@/pages/designations/DesignationsPage"
import BranchesPage      from "@/pages/branches/BranchesPage"
import AttendancePage    from "@/pages/attendance/AttendancePage"
import LeavePage         from "@/pages/leave/LeavePage"
import ApprovalsPage     from "@/pages/leave/ApprovalsPage"
import LeaveAdminPage    from "@/pages/leave/LeaveAdminPage"
import PayrollPage       from "@/pages/payroll/PayrollPage"
import OnboardingPage    from "@/pages/onboarding/OnboardingPage"
import UserAccountsPage  from "@/pages/users/UserAccountsPage"
import ForbiddenPage     from "@/pages/errors/ForbiddenPage"
import { useAuthStore } from "@/store/authStore"

function AuthenticatedChatBot() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  return isAuthenticated ? <ChatBot /> : null
}

const HR_ADMIN          = ["ROLE_HR_ADMIN"]
const MANAGER_AND_ABOVE = ["ROLE_HR_ADMIN", "ROLE_MANAGER"]
const ALL_ROLES         = ["ROLE_HR_ADMIN", "ROLE_MANAGER", "ROLE_EMPLOYEE"]

export default function App() {
  return (
    <BrowserRouter>
      <Toaster position="top-right" toastOptions={{ duration: 3000 }} />
      <Routes>
        {/* Public */}
        <Route element={<AuthLayout />}>
          <Route path="/login" element={<LoginPage />} />
        </Route>

        {/* 403 — rendered outside AppLayout so it always shows even if layout itself is broken */}
        <Route path="/403" element={<ForbiddenPage />} />

        {/* Any authenticated user */}
        <Route element={<ProtectedRoute allowedRoles={ALL_ROLES} />}>
          <Route element={<AppLayout />}>
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard"       element={<DashboardPage />} />
            <Route path="/attendance"      element={<AttendancePage />} />
            <Route path="/leave"           element={<LeavePage />} />
            <Route path="/onboarding"      element={<OnboardingPage />} />
          </Route>
        </Route>

        {/* Manager and above */}
        <Route element={<ProtectedRoute allowedRoles={MANAGER_AND_ABOVE} />}>
          <Route element={<AppLayout />}>
            <Route path="/employees"        element={<EmployeesPage />} />
            <Route path="/leave/approvals"  element={<ApprovalsPage />} />
          </Route>
        </Route>

        {/* HR Admin only */}
        <Route element={<ProtectedRoute allowedRoles={HR_ADMIN} />}>
          <Route element={<AppLayout />}>
            <Route path="/payroll"      element={<PayrollPage />} />
            <Route path="/departments"  element={<DepartmentsPage />} />
            <Route path="/designations" element={<DesignationsPage />} />
            <Route path="/branches"     element={<BranchesPage />} />
            <Route path="/users"        element={<UserAccountsPage />} />
            <Route path="/leave/admin"  element={<LeaveAdminPage />} />
          </Route>
        </Route>

        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
      <AuthenticatedChatBot />
    </BrowserRouter>
  )
}

import { Outlet, Navigate } from "react-router-dom"
import { useAuthStore } from "@/store/authStore"

export default function AuthLayout() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)

  // Already logged in → send to dashboard
  if (isAuthenticated) return <Navigate to="/dashboard" replace />

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-blue-950 to-slate-900 flex items-center justify-center p-4">
      <Outlet />
    </div>
  )
}

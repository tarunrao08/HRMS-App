import { Navigate, Outlet } from "react-router-dom"
import { useAuthStore } from "@/store/authStore"

interface Props {
  allowedRoles?: string[]
}

export default function ProtectedRoute({ allowedRoles }: Props) {
  const { isAuthenticated, user } = useAuthStore()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  if (allowedRoles && allowedRoles.length > 0) {
    const hasRole = user?.roles.some((r) => allowedRoles.includes(r)) ?? false
    if (!hasRole) {
      return <Navigate to="/403" replace />
    }
  }

  return <Outlet />
}

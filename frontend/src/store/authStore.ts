import { create } from "zustand"
import { persist } from "zustand/middleware"
import type { AuthUser, LoginResponse } from "@/types"

interface AuthStore {
  user: AuthUser | null
  accessToken: string | null
  refreshToken: string | null
  isAuthenticated: boolean
  login: (data: LoginResponse) => void
  logout: () => void
}

export const useAuthStore = create<AuthStore>()(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,

      login: (data) =>
        set({
          user: { id: data.employeeId, username: data.username, email: data.email, roles: data.roles },
          accessToken: data.accessToken,
          refreshToken: data.refreshToken,
          isAuthenticated: true,
        }),

      logout: () =>
        set({
          user: null,
          accessToken: null,
          refreshToken: null,
          isAuthenticated: false,
        }),
    }),
    { name: "hrms-auth" }
  )
)

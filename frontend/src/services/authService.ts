import axios from "axios"
import api from "./api"
import type { LoginRequest, LoginResponse } from "@/types"

// Uses plain axios (not the api instance) to avoid the auth interceptor on these calls
const authApi = axios.create({
  baseURL: "/api/auth",
  headers: { "Content-Type": "application/json" },
})

export const authService = {
  login: async (data: LoginRequest): Promise<LoginResponse> => {
    const res = await authApi.post("/login", data)
    return res.data.data as LoginResponse
  },

  refresh: async (refreshToken: string): Promise<LoginResponse> => {
    const res = await authApi.post("/refresh", { refreshToken })
    return res.data.data as LoginResponse
  },

  logout: async (): Promise<void> => {
    const stored = localStorage.getItem("hrms-auth")
    const token = stored ? JSON.parse(stored)?.state?.accessToken : null
    if (token) {
      await authApi.post("/logout", null, {
        headers: { Authorization: `Bearer ${token}` },
      }).catch(() => {/* ignore logout errors */})
    }
  },

  changePassword: async (currentPassword: string, newPassword: string): Promise<void> => {
    await api.post("/auth/change-password", { currentPassword, newPassword })
  },
}

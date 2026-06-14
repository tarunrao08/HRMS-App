import api from "./api"

export interface UserAccount {
  id: string
  username: string
  email: string
  employeeId?: string
  employeeName?: string
  employeeCode?: string
  roles: string[]
  enabled: boolean
  lastLoginAt?: string
  createdAt: string
}

export interface CreateUserPayload {
  employeeId: string
  username: string
  password: string
  role: string
}

export interface ResetPasswordPayload {
  newPassword: string
}

const userService = {
  getAll() { return api.get<UserAccount[]>("/users") },

  createUser(data: CreateUserPayload) {
    return api.post<UserAccount>("/users", data)
  },

  resetPassword(id: string, newPassword: string) {
    return api.post<UserAccount>(`/users/${id}/reset-password`, { newPassword })
  },
}

export default userService

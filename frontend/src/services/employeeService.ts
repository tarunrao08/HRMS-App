import api from "./api"
import type { PageableResponse } from "@/types"

export interface EmployeeRequest {
  firstName: string
  lastName: string
  email: string
  phone?: string
  departmentId: string
  designationId: string
  branchId: string
  joiningDate: string
  employmentType: string
  employmentStatus: string
  gender: string
  managerId?: string
  dateOfBirth?: string
  address?: string
  emergencyContactName?: string
  emergencyContactPhone?: string
}

export interface EmployeeResponse {
  id: string
  employeeCode: string
  firstName: string
  lastName: string
  email: string
  phone?: string
  departmentId: string
  departmentName: string
  designationId: string
  designationName: string
  branchId: string
  branchName: string
  managerId?: string
  managerName?: string
  joiningDate: string
  employmentType: string
  employmentStatus: string
  gender: string
  dateOfBirth?: string
  address?: string
  emergencyContactName?: string
  emergencyContactPhone?: string
  basicSalary?: number
  createdAt: string
}

export interface EmployeeFilter {
  search?: string
  departmentId?: string
  designationId?: string
  branchId?: string
  employmentStatus?: string
  employmentType?: string
  page?: number
  size?: number
}

const employeeService = {
  getAll(filter: EmployeeFilter = {}) {
    return api.get<PageableResponse<EmployeeResponse>>("/employees", { params: filter })
  },
  getById(id: string) {
    return api.get<EmployeeResponse>(`/employees/${id}`)
  },
  create(data: EmployeeRequest) {
    return api.post<EmployeeResponse>("/employees", data)
  },
  update(id: string, data: Partial<EmployeeRequest>) {
    return api.put<EmployeeResponse>(`/employees/${id}`, data)
  },
  delete(id: string) {
    return api.delete(`/employees/${id}`)
  },
  getSummaries() {
    return api.get<{ id: string; employeeCode: string; fullName: string }[]>("/employees/summaries")
  },
}

export default employeeService

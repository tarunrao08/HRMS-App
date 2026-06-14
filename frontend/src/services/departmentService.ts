import api from "./api"

export interface DepartmentRequest { name: string; description?: string }
export interface DepartmentResponse { id: string; name: string; description?: string; employeeCount?: number; createdAt: string }

const departmentService = {
  getAll() { return api.get<DepartmentResponse[]>("/departments") },
  getById(id: string) { return api.get<DepartmentResponse>(`/departments/${id}`) },
  create(data: DepartmentRequest) { return api.post<DepartmentResponse>("/departments", data) },
  update(id: string, data: DepartmentRequest) { return api.put<DepartmentResponse>(`/departments/${id}`, data) },
  delete(id: string) { return api.delete(`/departments/${id}`) },
}

export default departmentService

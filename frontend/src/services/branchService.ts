import api from "./api"

export interface BranchDepartmentRef { id: string; name: string }

export interface BranchRequest {
  name: string
  city: string
  state: string
  country: string
  address?: string
  // Full replace-all set of departments this branch offers.
  departmentIds?: string[]
}
export interface BranchResponse {
  id: string
  name: string
  city: string
  state: string
  country: string
  address?: string
  departments?: BranchDepartmentRef[]
  createdAt: string
}

const branchService = {
  getAll() { return api.get<BranchResponse[]>("/branches") },
  getById(id: string) { return api.get<BranchResponse>(`/branches/${id}`) },
  create(data: BranchRequest) { return api.post<BranchResponse>("/branches", data) },
  update(id: string, data: BranchRequest) { return api.put<BranchResponse>(`/branches/${id}`, data) },
  delete(id: string) { return api.delete(`/branches/${id}`) },
}

export default branchService

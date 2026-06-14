import api from "./api"

export interface DesignationRequest { name: string; departmentId: string; description?: string }
export interface DesignationResponse { id: string; name: string; departmentId: string; departmentName: string; description?: string; createdAt: string }

const designationService = {
  getAll(departmentId?: string) {
    return api.get<DesignationResponse[]>("/designations", { params: departmentId ? { departmentId } : undefined })
  },
  getById(id: string) { return api.get<DesignationResponse>(`/designations/${id}`) },
  create(data: DesignationRequest) { return api.post<DesignationResponse>("/designations", data) },
  update(id: string, data: DesignationRequest) { return api.put<DesignationResponse>(`/designations/${id}`, data) },
  delete(id: string) { return api.delete(`/designations/${id}`) },
}

export default designationService

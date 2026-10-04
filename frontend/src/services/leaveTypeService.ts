import api from "./api"

export type ApplicableGender = "MALE" | "FEMALE" | "OTHER"

export interface LeaveTypeRequest {
  name: string
  code: string
  description?: string
  maxDaysPerYear: number
  carryForwardAllowed: boolean
  maxCarryForwardDays: number
  encashmentAllowed: boolean
  paid: boolean
  requiresDocument: boolean
  minNoticeDays: number
  active: boolean
  tenureBased: boolean
  applicableGender?: ApplicableGender | null
}

export interface LeaveTypeResponse {
  id: string
  name: string
  code: string
  description?: string
  maxDaysPerYear: number
  carryForwardAllowed: boolean
  maxCarryForwardDays: number
  encashmentAllowed: boolean
  paid: boolean
  requiresDocument: boolean
  minNoticeDays: number
  active: boolean
  tenureBased: boolean
  applicableGender?: ApplicableGender | null
  createdAt: string
}

export interface LeaveTypeTenureTierRequest {
  minYears: number
  maxYears?: number | null
  days: number
}

export interface LeaveTypeTenureTierResponse {
  id: string
  leaveTypeId: string
  minYears: number
  maxYears?: number | null
  days: number
}

const leaveTypeService = {
  getAll() {
    return api.get<LeaveTypeResponse[]>("/leave-types")
  },
  getById(id: string) {
    return api.get<LeaveTypeResponse>(`/leave-types/${id}`)
  },
  create(data: LeaveTypeRequest) {
    return api.post<LeaveTypeResponse>("/leave-types", data)
  },
  update(id: string, data: LeaveTypeRequest) {
    return api.put<LeaveTypeResponse>(`/leave-types/${id}`, data)
  },
  delete(id: string) {
    return api.delete(`/leave-types/${id}`)
  },

  getTiers(leaveTypeId: string) {
    return api.get<LeaveTypeTenureTierResponse[]>(`/leave-types/${leaveTypeId}/tiers`)
  },
  addTier(leaveTypeId: string, data: LeaveTypeTenureTierRequest) {
    return api.post<LeaveTypeTenureTierResponse>(`/leave-types/${leaveTypeId}/tiers`, data)
  },
  updateTier(leaveTypeId: string, tierId: string, data: LeaveTypeTenureTierRequest) {
    return api.put<LeaveTypeTenureTierResponse>(`/leave-types/${leaveTypeId}/tiers/${tierId}`, data)
  },
  deleteTier(leaveTypeId: string, tierId: string) {
    return api.delete(`/leave-types/${leaveTypeId}/tiers/${tierId}`)
  },
}

export default leaveTypeService

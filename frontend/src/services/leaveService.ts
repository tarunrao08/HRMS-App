import api from "./api"
import type { PageableResponse } from "@/types"

export interface LeaveType {
  id: string
  name: string
  code: string
  description?: string
  maxDaysPerYear: number
  paid: boolean
}

export interface LeaveBalance {
  id: string
  employeeId: string
  leaveTypeId: string
  leaveTypeName: string
  leaveTypeCode: string
  year: number
  allocatedDays: number
  usedDays: number
  pendingDays: number
  availableDays: number
}

export interface LeaveRequest {
  id: string
  employeeId: string
  employeeName: string
  employeeCode: string
  leaveTypeId: string
  leaveTypeName: string
  startDate: string
  endDate: string
  totalDays: number
  halfDay: boolean
  reason: string
  status: string
  appliedAt: string
  currentApprovalLevel: number
  rejectionReason?: string
  cancelledAt?: string
}

export interface ApplyLeaveRequest {
  leaveTypeId: string
  startDate: string
  endDate: string
  halfDay?: boolean
  reason: string
  documentUrl?: string
}

const leaveService = {
  getLeaveTypes() {
    return api.get<LeaveType[]>("/leave/types")
  },

  getMyBalances() {
    return api.get<LeaveBalance[]>("/leave/balances/me")
  },

  getMyRequests(params: { status?: string; page?: number; size?: number } = {}) {
    return api.get<PageableResponse<LeaveRequest>>("/leave/requests/me", { params })
  },

  apply(data: ApplyLeaveRequest) {
    return api.post<LeaveRequest>("/leave/requests", data)
  },

  getPendingApprovals() {
    return api.get<LeaveRequest[]>("/leave/requests/pending-approval")
  },

  approve(id: string, comments?: string) {
    return api.patch<LeaveRequest>(`/leave/requests/${id}/approve`, { comments })
  },

  reject(id: string, comments: string) {
    return api.patch<LeaveRequest>(`/leave/requests/${id}/reject`, { comments })
  },

  cancel(id: string) {
    return api.patch<LeaveRequest>(`/leave/requests/${id}/cancel`)
  },

  getAllRequests(params: { employeeId?: string; status?: string; page?: number; size?: number } = {}) {
    return api.get<PageableResponse<LeaveRequest>>("/leave/requests", { params })
  },
}

export default leaveService

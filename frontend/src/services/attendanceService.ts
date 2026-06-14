import api from "./api"
import type { PageableResponse } from "@/types"

export interface AttendanceRecord {
  id: string
  employeeId: string
  employeeName: string
  employeeCode: string
  attendanceDate: string
  punchIn?: string
  punchOut?: string
  status: string
  workingHours?: number
  shiftId?: string
  shiftName?: string
  remarks?: string
}

export interface AttendanceFilter {
  employeeId?: string
  fromDate?: string
  toDate?: string
  status?: string
  page?: number
  size?: number
}

export interface MonthlySummary {
  employeeId: string
  employeeName: string
  month: number
  year: number
  totalWorkingDays: number
  presentDays: number
  absentDays: number
  halfDays: number
  lateDays: number
  totalWorkingHours: number
}

export interface ShiftResponse {
  id: string
  name: string
  startTime: string
  endTime: string
  description?: string
}

export interface TodayAttendanceRow {
  employeeId: string
  employeeName: string
  employeeCode: string
  departmentName?: string
  designationTitle?: string
  status?: "PRESENT" | "ABSENT" | "HALF_DAY" | string
  recordId?: string
}

const attendanceService = {
  getRecords(filter: AttendanceFilter = {}) {
    return api.get<PageableResponse<AttendanceRecord>>("/attendance", { params: filter })
  },
  getById(id: string) {
    return api.get<AttendanceRecord>(`/attendance/${id}`)
  },
  getMonthlySummaries(month: number, year: number) {
    return api.get<MonthlySummary[]>("/attendance/monthly-summary", { params: { month, year } })
  },
  getTodayAttendance() {
    return api.get<TodayAttendanceRow[]>("/attendance/today")
  },
  markAttendance(employeeId: string, status: string) {
    return api.post<TodayAttendanceRow>("/attendance/mark", { employeeId, status })
  },
}

export default attendanceService

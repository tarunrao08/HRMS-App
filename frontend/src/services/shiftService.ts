import api from "./api"

export interface ShiftResponse {
  id: string
  name: string
  startTime: string
  endTime: string
  gracePeriodMinutes: number
  workingHours: number
  nightShift: boolean
  active: boolean
}

export interface EmployeeShiftResponse {
  id: string
  employeeId: string
  employeeName: string
  shiftId: string
  shiftName: string
  effectiveFrom: string
  effectiveTo?: string
}

const shiftService = {
  getAll() {
    return api.get<ShiftResponse[]>("/shifts")
  },
}

export default shiftService

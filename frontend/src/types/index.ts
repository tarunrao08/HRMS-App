// ── Auth ─────────────────────────────────────────────────────────────────────

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  username: string
  email: string
  roles: string[]
  employeeId?: string
}

export interface AuthUser {
  id?: string
  username: string
  email: string
  roles: string[]
}

// ── API envelope ──────────────────────────────────────────────────────────────

export interface ApiResponse<T> {
  success: boolean
  message?: string
  data?: T
  errorCode?: string
  timestamp: string
}

export interface PageableResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
  first: boolean
}

// ── Employee ─────────────────────────────────────────────────────────────────

export type EmploymentStatus = "ACTIVE" | "INACTIVE" | "RESIGNED" | "TERMINATED"
export type EmploymentType   = "FULL_TIME" | "PART_TIME" | "CONTRACT" | "INTERN"
export type Gender           = "MALE" | "FEMALE" | "OTHER"

export interface EmployeeSummary {
  id: string
  employeeCode: string
  firstName: string
  lastName: string
  email: string
  phone?: string
  joiningDate: string
  employmentStatus: EmploymentStatus
  employmentType: EmploymentType
  departmentName?: string
  designationTitle?: string
  branchName?: string
  profilePictureUrl?: string
}

export interface Department {
  id: string
  name: string
  description?: string
  createdAt: string
}

export interface Designation {
  id: string
  title: string
  description?: string
  createdAt: string
}

export interface Branch {
  id: string
  name: string
  city?: string
  state?: string
  country: string
  createdAt: string
}

// ── Attendance ────────────────────────────────────────────────────────────────

export type AttendanceStatus =
  | "PRESENT" | "ABSENT" | "LATE" | "HALF_DAY"
  | "HOLIDAY" | "WEEKEND" | "ON_LEAVE" | "WORK_FROM_HOME" | "REGULARIZED"

export interface AttendanceRecord {
  id: string
  employeeId: string
  employeeName: string
  attendanceDate: string
  punchIn?: string
  punchOut?: string
  workingHours?: number
  status: AttendanceStatus
  regularized: boolean
}

// ── Leave ────────────────────────────────────────────────────────────────────

export type LeaveRequestStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED"

export interface LeaveBalance {
  id: string
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
  leaveTypeName: string
  startDate: string
  endDate: string
  totalDays: number
  reason: string
  status: LeaveRequestStatus
  appliedAt: string
}

// ── Payroll ───────────────────────────────────────────────────────────────────

export type PayrollRunStatus = "DRAFT" | "PROCESSED" | "APPROVED" | "DISBURSED"

export interface PayrollRun {
  id: string
  year: number
  month: number
  status: PayrollRunStatus
  totalEmployees: number
  totalGross: number
  totalNet: number
  createdAt: string
}

export interface Payslip {
  id: string
  employeeId: string
  employeeName: string
  year: number
  month: number
  grossSalary: number
  netSalary: number
  published: boolean
}

import api from "./api"
import type { PageableResponse } from "@/types"

export type Relationship =
  | "FATHER" | "MOTHER" | "SPOUSE" | "SON" | "DAUGHTER" | "BROTHER" | "SISTER" | "OTHER"

// ── Family members ───────────────────────────────────────────────────────────

export interface FamilyMemberRequest {
  name: string
  relationship: Relationship | ""
  dateOfBirth?: string
  gender?: string
  occupation?: string
  contactNumber?: string
}

export interface FamilyMemberResponse {
  id: string
  employeeId: string
  name: string
  relationship: Relationship
  dateOfBirth?: string
  gender?: string
  occupation?: string
  contactNumber?: string
}

// ── Nominees ──────────────────────────────────────────────────────────────────

export interface NomineeRequest {
  name: string
  relationship: Relationship | ""
  dateOfBirth?: string
  sharePercentage: number | ""
  address?: string
  contactNumber?: string
  minor: boolean
  guardianName?: string
  guardianRelationship?: Relationship | ""
}

export interface NomineeResponse {
  id: string
  employeeId: string
  name: string
  relationship: Relationship
  dateOfBirth?: string
  sharePercentage: number
  address?: string
  contactNumber?: string
  minor: boolean
  guardianName?: string
  guardianRelationship?: Relationship
}

// ── Employee ──────────────────────────────────────────────────────────────────

export interface EmployeeRequest {
  firstName: string
  lastName: string
  email: string
  phone?: string
  departmentId: string
  designationId: string
  branchId: string
  joiningDate: string
  resignationDate?: string
  employmentType: string
  employmentStatus: string
  gender: string
  managerId?: string
  shiftId?: string
  dateOfBirth?: string
  address?: string
  city?: string
  state?: string
  pincode?: string
  profilePictureUrl?: string
  panNumber?: string
  aadharNumber?: string
  bankAccountNumber?: string
  bankIfscCode?: string
  bankName?: string
  emergencyContactName?: string
  emergencyContactPhone?: string
  emergencyContactRelation?: string
  familyMembers?: FamilyMemberRequest[]
  nominees?: NomineeRequest[]
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
  designationTitle: string
  branchId: string
  branchName: string
  managerId?: string
  managerName?: string
  joiningDate: string
  resignationDate?: string
  employmentType: string
  employmentStatus: string
  gender: string
  dateOfBirth?: string
  address?: string
  city?: string
  state?: string
  pincode?: string
  profilePictureUrl?: string
  panNumber?: string
  aadharNumber?: string
  bankAccountNumber?: string
  bankIfscCode?: string
  bankName?: string
  emergencyContactName?: string
  emergencyContactPhone?: string
  emergencyContactRelation?: string
  familyMembers?: FamilyMemberResponse[]
  nominees?: NomineeResponse[]
  basicSalary?: number
  createdAt: string
}

export interface EmployeeNameResponse {
  id: string
  employeeCode: string
  fullName: string
  departmentId?: string
  managerial?: boolean
}

export interface EmployeeSummary {
  id: string
  employeeCode: string
  firstName: string
  lastName: string
  departmentName?: string
  designationTitle?: string
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
    return api.get<EmployeeNameResponse[]>("/employees/summaries")
  },
  getDirectReports(managerId: string) {
    return api.get<EmployeeSummary[]>(`/employees/${managerId}/reports`)
  },

  // ── Family members (standalone, for editing after creation) ─────────────────
  getFamilyMembers(employeeId: string) {
    return api.get<FamilyMemberResponse[]>(`/employees/${employeeId}/family-members`)
  },
  addFamilyMember(employeeId: string, data: FamilyMemberRequest) {
    return api.post<FamilyMemberResponse>(`/employees/${employeeId}/family-members`, data)
  },
  updateFamilyMember(employeeId: string, memberId: string, data: FamilyMemberRequest) {
    return api.put<FamilyMemberResponse>(`/employees/${employeeId}/family-members/${memberId}`, data)
  },
  deleteFamilyMember(employeeId: string, memberId: string) {
    return api.delete(`/employees/${employeeId}/family-members/${memberId}`)
  },

  // ── Nominees (standalone, for editing after creation) ────────────────────────
  getNominees(employeeId: string) {
    return api.get<NomineeResponse[]>(`/employees/${employeeId}/nominees`)
  },
  addNominee(employeeId: string, data: NomineeRequest) {
    return api.post<NomineeResponse>(`/employees/${employeeId}/nominees`, data)
  },
  updateNominee(employeeId: string, nomineeId: string, data: NomineeRequest) {
    return api.put<NomineeResponse>(`/employees/${employeeId}/nominees/${nomineeId}`, data)
  },
  deleteNominee(employeeId: string, nomineeId: string) {
    return api.delete(`/employees/${employeeId}/nominees/${nomineeId}`)
  },
}

export default employeeService

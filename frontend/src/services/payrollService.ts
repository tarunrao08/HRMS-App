import api from "./api"
import type { PageableResponse } from "@/types"

// ── API shape mirrors PayrollRunResponse / PayslipResponse DTOs ──────────────

export interface PayrollRun {
  id: string
  year: number
  month: number
  status: "DRAFT" | "PROCESSED" | "APPROVED" | "DISBURSED"
  totalEmployees: number
  totalGross: number
  totalDeductions: number
  totalNet: number
  runDate?: string
  approvedAt?: string
  remarks?: string
  createdAt: string
}

// One earning line on a payslip (pro-rated) — mirrors PayslipComponentResponse
export interface PayslipComponentResponse {
  payrollComponentId: string
  code: string
  name: string
  componentType: "EARNING" | "DEDUCTION" | "STATUTORY"
  amount: number
  displayOrder: number
}

export interface Payslip {
  id: string
  payrollRunId: string
  employeeId: string
  employeeName: string
  employeeCode: string
  year: number
  month: number
  // Earnings — open-ended, catalog-driven (see PayslipComponentResponse)
  components: PayslipComponentResponse[]
  grossSalary: number
  // Deductions
  tds: number
  otherDeductions: number
  totalDeductions: number
  // Days
  workingDays: number
  paidDays: number
  lopDays: number
  // Result
  netSalary: number
  pdfUrl?: string
  published: boolean
  publishedAt?: string
  createdAt: string
}

export type PayrollComponentType = "EARNING" | "DEDUCTION" | "STATUTORY"
export type CalculationType = "FIXED" | "PERCENTAGE" | "FORMULA"

// Catalog entry — mirrors PayrollComponentResponse
export interface PayrollComponent {
  id: string
  name: string
  code: string
  componentType: PayrollComponentType
  calculationType: CalculationType
  value: number | null
  percentageOfComponentId?: string | null
  percentageOfComponentCode?: string | null
  taxable: boolean
  active: boolean
  displayOrder: number
  createdAt: string
}

// One resolved earning line on a salary structure — mirrors SalaryStructureComponentResponse
export interface SalaryStructureComponentResponse {
  payrollComponentId: string
  code: string
  name: string
  componentType: PayrollComponentType
  calculationType: CalculationType
  value: number | null
  computedAmount: number
  displayOrder: number
}

// Per-employee override for one earning component — mirrors SalaryStructureComponentInput
export interface SalaryStructureComponentInput {
  payrollComponentId: string
  calculationType?: CalculationType
  value?: number | null
}

export interface SalaryStructureResponse {
  id: string
  employeeId: string
  employeeName?: string
  effectiveFrom: string
  effectiveTo?: string
  annualCtc: number
  monthlyGross: number
  components: SalaryStructureComponentResponse[]
  pfEmployee: number
  esiEmployee: number
  professionalTax: number
  netSalary: number
  pfApplicable: boolean
  esiApplicable: boolean
  active: boolean
  createdAt: string
}

export interface CreateSalaryStructureRequest {
  employeeId: string
  annualCtc: number
  effectiveFrom: string
  components?: SalaryStructureComponentInput[]
}

export interface PayableSummaryResponse {
  totalActiveEmployees: number
  totalMonthlyPayable: number
  totalAnnualCtc: number
}

const payrollService = {
  getRuns() {
    return api.get<PageableResponse<PayrollRun>>("/payroll/runs")
  },
  createRun(month: number, year: number, remarks?: string) {
    return api.post<PayrollRun>("/payroll/runs", { month, year, remarks })
  },
  processRun(id: string) {
    return api.post<PayrollRun>(`/payroll/runs/${id}/process`)
  },
  approveRun(id: string) {
    return api.post<PayrollRun>(`/payroll/runs/${id}/approve`)
  },
  getPayslips(runId: string) {
    return api.get<Payslip[]>(`/payroll/runs/${runId}/payslips`)
  },
  // Salary structures
  getSalaryStructures(employeeId: string) {
    return api.get<SalaryStructureResponse[]>(`/payroll/salary-structures/employee/${employeeId}`)
  },
  getActiveSalaryStructure(employeeId: string) {
    return api.get<SalaryStructureResponse>(`/payroll/salary-structures/employee/${employeeId}/active`)
  },
  createSalaryStructure(data: CreateSalaryStructureRequest) {
    return api.post<SalaryStructureResponse>("/payroll/salary-structures", data)
  },
  getMySalaryStructure() {
    return api.get<SalaryStructureResponse>("/payroll/salary-structures/my")
  },
  getTotalPayableSummary() {
    return api.get<PayableSummaryResponse>("/payroll/salary-structures/summary")
  },
  // Payroll component catalog
  getActiveComponents() {
    return api.get<PayrollComponent[]>("/payroll/components/active")
  },
  generatePayslip(employeeId: string, month: number, year: number) {
    return api.post<Payslip>("/payroll/payslips/generate", { employeeId, month, year })
  },
  getMyLatestPayslip() {
    return api.get<Payslip>("/payroll/payslips/my/latest")
  },
  downloadPayslipPdf(payslipId: string) {
    return api.get<Blob>(`/payroll/payslips/${payslipId}/pdf`, { responseType: "blob" })
  },
  payslipPdfUrl(payslipId: string) {
    return `/api/payroll/payslips/${payslipId}/pdf`
  },
}

export default payrollService

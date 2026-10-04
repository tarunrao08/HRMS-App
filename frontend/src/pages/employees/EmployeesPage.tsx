import React, { useEffect, useRef, useState, useCallback } from "react"
import { Pencil, Trash2, Plus, X, ChevronLeft, ChevronRight } from "lucide-react"
import toast, { Toaster } from "react-hot-toast"

import PageHeader from "@/components/shared/PageHeader"
import EmptyState from "@/components/shared/EmptyState"
import ConfirmDialog from "@/components/shared/ConfirmDialog"
import Pagination from "@/components/shared/Pagination"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { Checkbox } from "@/components/ui/checkbox"
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs"
import {
  Table, TableHeader, TableBody, TableRow, TableHead, TableCell,
} from "@/components/ui/table"
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogClose,
} from "@/components/ui/dialog"
import {
  Select, SelectTrigger, SelectValue, SelectContent, SelectItem,
} from "@/components/ui/select"
import { Combobox } from "@/components/ui/combobox"

import employeeService from "@/services/employeeService"
import type {
  EmployeeFilter, EmployeeRequest, EmployeeResponse, EmployeeNameResponse,
  FamilyMemberRequest, NomineeRequest, Relationship,
} from "@/services/employeeService"
import departmentService from "@/services/departmentService"
import type { DepartmentResponse } from "@/services/departmentService"
import designationService from "@/services/designationService"
import type { DesignationResponse } from "@/services/designationService"
import branchService from "@/services/branchService"
import type { BranchResponse } from "@/services/branchService"
import shiftService from "@/services/shiftService"
import type { ShiftResponse } from "@/services/shiftService"
import attendanceService from "@/services/attendanceService"
import { toastApiError } from "@/services/api"
import { useIsHrAdmin } from "@/hooks/useRole"

// ── Types ─────────────────────────────────────────────────────────────────────

type EmploymentStatus = "ACTIVE" | "INACTIVE"
type EmploymentType   = "FULL_TIME" | "PART_TIME" | "CONTRACT" | "INTERN"
type Gender           = "MALE" | "FEMALE" | "OTHER"

const MIN_AGE = 21

const TAB_ORDER = ["basic", "documents", "family", "nominees"] as const
type TabKey = typeof TAB_ORDER[number]
const TAB_LABELS: Record<TabKey, string> = {
  basic: "Basic Info",
  documents: "Documents & Bank",
  family: "Family",
  nominees: "Nominees",
}

const RELATIONSHIP_OPTIONS: { value: Relationship; label: string }[] = [
  { value: "FATHER",   label: "Father" },
  { value: "MOTHER",   label: "Mother" },
  { value: "SPOUSE",   label: "Spouse" },
  { value: "SON",      label: "Son" },
  { value: "DAUGHTER", label: "Daughter" },
  { value: "BROTHER",  label: "Brother" },
  { value: "SISTER",   label: "Sister" },
  { value: "OTHER",    label: "Other" },
]

const PAN_REGEX     = /^[A-Z]{5}[0-9]{4}[A-Z]{1}$/
const AADHAR_REGEX  = /^\d{12}$/
const IFSC_REGEX    = /^[A-Z]{4}0[A-Z0-9]{6}$/
const PHONE_REGEX   = /^[6-9]\d{9}$/
const PINCODE_REGEX = /^[1-9]\d{5}$/
const EMAIL_REGEX   = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/** Strips a leading +91/91/0 country/trunk prefix down to a bare 10-digit Indian mobile number. */
function normalizePhone(raw: string): string {
  let digits = raw.replace(/[\s-]/g, "")
  if (digits.startsWith("+91")) digits = digits.slice(3)
  else if (digits.startsWith("91") && digits.length === 12) digits = digits.slice(2)
  else if (digits.startsWith("0") && digits.length === 11) digits = digits.slice(1)
  return digits.replace(/\D/g, "").slice(0, 10)
}

function computeAge(dob: string): number | null {
  if (!dob) return null
  const today = new Date()
  const birth = new Date(dob)
  let age = today.getFullYear() - birth.getFullYear()
  const monthDiff = today.getMonth() - birth.getMonth()
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) age--
  return age
}

/** A nominee is legally a minor once their computed age from DOB drops below 18. */
function isMinorAge(dob: string): boolean {
  const age = computeAge(dob)
  return age !== null && age < 18
}

function maxDobForAge(minAge: number): string {
  const d = new Date()
  d.setFullYear(d.getFullYear() - minAge)
  return d.toISOString().split("T")[0]
}

function todayStr(): string {
  return new Date().toISOString().split("T")[0]
}

function newKey(): string {
  return typeof crypto !== "undefined" && crypto.randomUUID ? crypto.randomUUID() : `k${Math.random()}`
}

// ── Family member row ─────────────────────────────────────────────────────────

interface FamilyMemberRow {
  key: string
  name: string
  relationship: Relationship | ""
  dateOfBirth: string
  gender: Gender | ""
  occupation: string
  contactNumber: string
}

function emptyFamilyRow(): FamilyMemberRow {
  return { key: newKey(), name: "", relationship: "", dateOfBirth: "", gender: "", occupation: "", contactNumber: "" }
}

function isFamilyRowBlank(row: FamilyMemberRow): boolean {
  return (
    !row.name.trim() && !row.relationship && !row.dateOfBirth &&
    !row.gender && !row.occupation.trim() && !row.contactNumber.trim()
  )
}

interface FamilyFieldErrors {
  name?: string
  relationship?: string
  dateOfBirth?: string
  contactNumber?: string
}

/** Per-field errors so each message can be shown right under its own input instead of one row-level line. */
function familyRowFieldErrors(row: FamilyMemberRow): FamilyFieldErrors {
  if (isFamilyRowBlank(row)) return {}
  const errors: FamilyFieldErrors = {}
  if (!row.name.trim()) errors.name = "Name is required"
  if (!row.relationship) errors.relationship = "Relationship is required"
  if (row.dateOfBirth && row.dateOfBirth >= todayStr()) errors.dateOfBirth = "Date of birth must be in the past"
  if (row.contactNumber && !PHONE_REGEX.test(row.contactNumber)) {
    errors.contactNumber = "Enter a valid 10-digit Indian mobile number"
  }
  return errors
}

function familyRowError(row: FamilyMemberRow): string | null {
  const e = familyRowFieldErrors(row)
  return e.name ?? e.relationship ?? e.dateOfBirth ?? e.contactNumber ?? null
}

// ── Nominee row ───────────────────────────────────────────────────────────────

interface NomineeRow {
  key: string
  name: string
  relationship: Relationship | ""
  dateOfBirth: string
  sharePercentage: string
  address: string
  contactNumber: string
  minor: boolean
  guardianName: string
  guardianRelationship: Relationship | ""
}

function emptyNomineeRow(): NomineeRow {
  return {
    key: newKey(), name: "", relationship: "", dateOfBirth: "", sharePercentage: "",
    address: "", contactNumber: "", minor: false, guardianName: "", guardianRelationship: "",
  }
}

function isNomineeRowBlank(row: NomineeRow): boolean {
  return (
    !row.name.trim() && !row.relationship && !row.sharePercentage.trim() &&
    !row.dateOfBirth && !row.address.trim() && !row.contactNumber.trim()
  )
}

interface NomineeFieldErrors {
  name?: string
  relationship?: string
  dateOfBirth?: string
  sharePercentage?: string
  contactNumber?: string
  guardianName?: string
  guardianRelationship?: string
}

/**
 * Per-field errors so each message can be shown right under its own input instead of one row-level line.
 * DOB is mandatory for nominees because the minor flag is derived from it (see updateNomineeRow) — without
 * a DOB there's no reliable way to know whether guardian details are legally required.
 */
function nomineeRowFieldErrors(row: NomineeRow): NomineeFieldErrors {
  if (isNomineeRowBlank(row)) return {}
  const errors: NomineeFieldErrors = {}
  if (!row.name.trim()) errors.name = "Name is required"
  if (!row.relationship) errors.relationship = "Relationship is required"
  if (!row.dateOfBirth) errors.dateOfBirth = "Date of birth is required"
  else if (row.dateOfBirth >= todayStr()) errors.dateOfBirth = "Date of birth must be in the past"
  const share = parseFloat(row.sharePercentage)
  if (!row.sharePercentage.trim() || Number.isNaN(share)) errors.sharePercentage = "Share percentage is required"
  else if (share < 0 || share > 100) errors.sharePercentage = "Share percentage must be between 0 and 100"
  if (row.contactNumber && !PHONE_REGEX.test(row.contactNumber)) {
    errors.contactNumber = "Enter a valid 10-digit Indian mobile number"
  }
  if (row.minor) {
    if (!row.guardianName.trim()) errors.guardianName = "Guardian name is required for a minor nominee"
    if (!row.guardianRelationship) errors.guardianRelationship = "Guardian relationship is required for a minor nominee"
  }
  return errors
}

function nomineeRowError(row: NomineeRow): string | null {
  const e = nomineeRowFieldErrors(row)
  return (
    e.name ?? e.relationship ?? e.dateOfBirth ?? e.sharePercentage ??
    e.contactNumber ?? e.guardianName ?? e.guardianRelationship ?? null
  )
}

function nomineeShareTotal(rows: NomineeRow[]): number {
  const total = rows
    .filter((r) => !isNomineeRowBlank(r))
    .reduce((sum, r) => sum + (parseFloat(r.sharePercentage) || 0), 0)
  return Math.round(total * 100) / 100
}

// ── Form state ────────────────────────────────────────────────────────────────

interface FormState {
  firstName: string
  lastName: string
  email: string
  phone: string
  dateOfBirth: string
  departmentId: string
  designationId: string
  branchId: string
  managerId: string
  shiftId: string
  joiningDate: string
  resignationDate: string
  employmentType: EmploymentType | ""
  employmentStatus: EmploymentStatus | ""
  gender: Gender | ""
  address: string
  city: string
  state: string
  pincode: string
  profilePictureUrl: string
  panNumber: string
  aadharNumber: string
  bankAccountNumber: string
  bankIfscCode: string
  bankName: string
  emergencyContactName: string
  emergencyContactPhone: string
  emergencyContactRelation: string
  familyMembers: FamilyMemberRow[]
  nominees: NomineeRow[]
}

const EMPTY_FORM: FormState = {
  firstName: "",
  lastName: "",
  email: "",
  phone: "",
  dateOfBirth: "",
  departmentId: "",
  designationId: "",
  branchId: "",
  managerId: "",
  shiftId: "",
  joiningDate: "",
  resignationDate: "",
  employmentType: "",
  employmentStatus: "",
  gender: "",
  address: "",
  city: "",
  state: "",
  pincode: "",
  profilePictureUrl: "",
  panNumber: "",
  aadharNumber: "",
  bankAccountNumber: "",
  bankIfscCode: "",
  bankName: "",
  emergencyContactName: "",
  emergencyContactPhone: "",
  emergencyContactRelation: "",
  familyMembers: [],
  nominees: [],
}

function firstMissingBasicField(f: FormState): string | null {
  if (!f.firstName.trim())      return "First name is required"
  if (!f.lastName.trim())       return "Last name is required"
  if (!f.email.trim())          return "Email is required"
  if (!f.phone.trim())          return "Phone number is required"
  if (!f.dateOfBirth)           return "Date of birth is required"
  if (!f.departmentId)          return "Department is required"
  if (!f.designationId)         return "Designation is required"
  if (!f.branchId)              return "Branch is required"
  if (!f.joiningDate)           return "Joining date is required"
  if (!f.gender)                return "Gender is required"
  if (!f.employmentType)        return "Employment type is required"
  if (!f.employmentStatus)      return "Employment status is required"
  return null
}

// ── Badge helper ──────────────────────────────────────────────────────────────

function statusBadgeVariant(
  status: string
): "success" | "secondary" | "warning" | "destructive" | "default" {
  switch (status) {
    case "ACTIVE":   return "success"
    case "INACTIVE": return "secondary"
    default:         return "default"
  }
}

// ── Main Page ─────────────────────────────────────────────────────────────────

export default function EmployeesPage() {
  const isHrAdmin = useIsHrAdmin()

  // List state
  const [employees, setEmployees]       = useState<EmployeeResponse[]>([])
  const [loading, setLoading]           = useState(true)
  const [page, setPage]                 = useState(0)
  const [totalPages, setTotalPages]     = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const PAGE_SIZE = 10

  // Filter state
  const [searchInput, setSearchInput]       = useState("")
  const [search, setSearch]                 = useState("")
  const [departmentFilter, setDepartmentFilter] = useState<string>("")
  const [branchFilter, setBranchFilter]     = useState<string>("")
  const [statusFilter, setStatusFilter]     = useState<string>("")

  // Reference data
  // Department dropdown in the filter bar — scoped to branchFilter's departments when a branch
  // filter is picked, all departments otherwise.
  const [filterDepartments, setFilterDepartments] = useState<DepartmentResponse[]>([])
  const [designations, setDesignations] = useState<DesignationResponse[]>([])
  const [branches, setBranches]         = useState<BranchResponse[]>([])
  const [shifts, setShifts]             = useState<ShiftResponse[]>([])
  const [empSummaries, setEmpSummaries] = useState<EmployeeNameResponse[]>([])

  // Dialog state
  const [dialogOpen, setDialogOpen]         = useState(false)
  const [editTarget, setEditTarget]         = useState<EmployeeResponse | null>(null)
  const [form, setForm]                     = useState<FormState>(EMPTY_FORM)
  // Department dropdown in the create/edit dialog — only the departments form.branchId offers.
  const [dialogDepartments, setDialogDepartments] = useState<DepartmentResponse[]>([])
  const [dialogDesignations, setDialogDesignations] = useState<DesignationResponse[]>([])
  const [submitting, setSubmitting]         = useState(false)
  const [loadingEdit, setLoadingEdit]       = useState(false)
  const [activeTab, setActiveTab]           = useState<TabKey>("basic")
  const [maxReachedIndex, setMaxReachedIndex] = useState(0)
  const [basicAttempted, setBasicAttempted] = useState(false)
  const [showCancelConfirm, setShowCancelConfirm] = useState(false)
  const initialFormRef = useRef<FormState>(EMPTY_FORM)

  // Delete confirm
  const [deleteTarget, setDeleteTarget] = useState<EmployeeResponse | null>(null)
  const [deleting, setDeleting]         = useState(false)

  // Debounce search
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const handleSearchInput = (val: string) => {
    setSearchInput(val)
    if (debounceRef.current) clearTimeout(debounceRef.current)
    debounceRef.current = setTimeout(() => {
      setSearch(val)
      setPage(0)
    }, 400)
  }

  // ── Data loading ────────────────────────────────────────────────────────────

  const fetchEmployees = useCallback(() => {
    setLoading(true)
    const filter: EmployeeFilter = {
      page,
      size: PAGE_SIZE,
      ...(search          ? { search }                                    : {}),
      ...(departmentFilter ? { departmentId: departmentFilter }           : {}),
      ...(branchFilter     ? { branchId: branchFilter }                   : {}),
      ...(statusFilter    ? { employmentStatus: statusFilter }            : {}),
    }
    employeeService
      .getAll(filter)
      .then((res) => {
        const data = res.data
        // Handle both direct PageableResponse and wrapped ApiResponse
        const paged = (data as any).data ?? data
        setEmployees(paged.content ?? [])
        setTotalPages(paged.totalPages ?? 0)
        setTotalElements(paged.totalElements ?? 0)
      })
      .catch(() => {
        setEmployees([])
      })
      .finally(() => setLoading(false))
  }, [page, search, departmentFilter, branchFilter, statusFilter])

  useEffect(() => { fetchEmployees() }, [fetchEmployees])

  // Load reference data once
  useEffect(() => {
    branchService.getAll().then((r) => {
      const data = (r.data as any).data ?? r.data
      setBranches(Array.isArray(data) ? data : [])
    }).catch(() => setBranches([]))

    shiftService.getAll().then((r) => {
      const data = (r.data as any).data ?? r.data
      setShifts(Array.isArray(data) ? data : [])
    }).catch(() => setShifts([]))

    employeeService.getSummaries().then((r) => {
      const data = (r.data as any).data ?? r.data
      setEmpSummaries(Array.isArray(data) ? data : [])
    }).catch(() => setEmpSummaries([]))
  }, [])

  // Load the filter bar's department dropdown, scoped to the selected branch (all departments
  // when no branch filter is picked — unlike creation, viewing/filtering isn't branch-constrained).
  useEffect(() => {
    departmentService.getAll(branchFilter || undefined).then((r) => {
      const data = (r.data as any).data ?? r.data
      setFilterDepartments(Array.isArray(data) ? data : [])
    }).catch(() => setFilterDepartments([]))
  }, [branchFilter])

  // Load the create/edit dialog's department dropdown, scoped to the selected branch — an
  // employee can only be placed in a department that branch actually offers.
  useEffect(() => {
    if (!form.branchId) {
      setDialogDepartments([])
      return
    }
    departmentService.getAll(form.branchId).then((r) => {
      const data = (r.data as any).data ?? r.data
      setDialogDepartments(Array.isArray(data) ? data : [])
    }).catch(() => setDialogDepartments([]))
  }, [form.branchId])

  // Load designations when dialog dept changes
  useEffect(() => {
    if (!form.departmentId) {
      setDialogDesignations([])
      return
    }
    designationService.getAll(form.departmentId).then((r) => {
      const data = (r.data as any).data ?? r.data
      setDialogDesignations(Array.isArray(data) ? data : [])
    }).catch(() => setDialogDesignations([]))
  }, [form.departmentId])

  // ── Dialog helpers ──────────────────────────────────────────────────────────

  function openCreate() {
    setEditTarget(null)
    setForm(EMPTY_FORM)
    initialFormRef.current = EMPTY_FORM
    setDialogDesignations([])
    setActiveTab("basic")
    setMaxReachedIndex(0)
    setBasicAttempted(false)
    setShowCancelConfirm(false)
    setDialogOpen(true)
  }

  async function openEdit(emp: EmployeeResponse) {
    setLoadingEdit(true)
    try {
      const res = await employeeService.getById(emp.id)
      const full = ((res.data as any).data ?? res.data) as EmployeeResponse

      const today = new Date().toISOString().slice(0, 10)
      let currentShiftId = ""
      try {
        const shiftRes = await attendanceService.getEmployeeShifts(emp.id)
        const assignments = (shiftRes.data as any).data ?? shiftRes.data
        // Not-yet-ended assignment with the latest effectiveFrom — covers shifts that
        // start on a future joining date, not just ones already active today.
        const current = (Array.isArray(assignments) ? assignments : [])
          .filter((a: { effectiveTo?: string }) => !a.effectiveTo || a.effectiveTo >= today)
          .sort((a: { effectiveFrom: string }, b: { effectiveFrom: string }) =>
            a.effectiveFrom < b.effectiveFrom ? 1 : -1
          )[0]
        currentShiftId = current?.shiftId ?? ""
      } catch {
        // Non-fatal — shift field just starts blank if this lookup fails.
      }

      setEditTarget(full)
      const loadedForm: FormState = {
        firstName:        full.firstName,
        lastName:         full.lastName,
        email:            full.email,
        phone:            full.phone ?? "",
        dateOfBirth:      full.dateOfBirth ?? "",
        departmentId:     full.departmentId,
        designationId:    full.designationId,
        branchId:         full.branchId,
        managerId:        full.managerId ?? "",
        shiftId:          currentShiftId,
        joiningDate:      full.joiningDate,
        resignationDate:  full.resignationDate ?? "",
        employmentType:   (full.employmentType as EmploymentType) || "",
        employmentStatus: (full.employmentStatus as EmploymentStatus) || "",
        gender:           (full.gender as Gender) || "",
        address:          full.address ?? "",
        city:             full.city ?? "",
        state:            full.state ?? "",
        pincode:          full.pincode ?? "",
        profilePictureUrl: full.profilePictureUrl ?? "",
        panNumber:        full.panNumber ?? "",
        aadharNumber:     full.aadharNumber ?? "",
        bankAccountNumber: full.bankAccountNumber ?? "",
        bankIfscCode:     full.bankIfscCode ?? "",
        bankName:         full.bankName ?? "",
        emergencyContactName:     full.emergencyContactName ?? "",
        emergencyContactPhone:    full.emergencyContactPhone ?? "",
        emergencyContactRelation: full.emergencyContactRelation ?? "",
        familyMembers: (full.familyMembers ?? []).map((m) => ({
          key: newKey(),
          name: m.name,
          relationship: m.relationship,
          dateOfBirth: m.dateOfBirth ?? "",
          gender: (m.gender as Gender) || "",
          occupation: m.occupation ?? "",
          contactNumber: m.contactNumber ?? "",
        })),
        nominees: (full.nominees ?? []).map((n) => ({
          key: newKey(),
          name: n.name,
          relationship: n.relationship,
          dateOfBirth: n.dateOfBirth ?? "",
          sharePercentage: String(n.sharePercentage),
          address: n.address ?? "",
          contactNumber: n.contactNumber ?? "",
          minor: n.minor,
          guardianName: n.guardianName ?? "",
          guardianRelationship: n.guardianRelationship ?? "",
        })),
      }
      setForm(loadedForm)
      initialFormRef.current = loadedForm
      setActiveTab("basic")
      setMaxReachedIndex(0)
      setBasicAttempted(false)
      setShowCancelConfirm(false)
      setDialogOpen(true)
    } catch (err) {
      toastApiError(err, "Failed to load employee details")
    } finally {
      setLoadingEdit(false)
    }
  }

  function isFormDirty(): boolean {
    return JSON.stringify(form) !== JSON.stringify(initialFormRef.current)
  }

  function handleDialogOpenChange(open: boolean) {
    if (open) {
      setDialogOpen(true)
      return
    }
    // Attempted close (Cancel, X, Escape, or overlay click) — confirm if the user has changed anything.
    if (isFormDirty()) {
      setShowCancelConfirm(true)
    } else {
      setDialogOpen(false)
    }
  }

  function confirmCancel() {
    setShowCancelConfirm(false)
    setDialogOpen(false)
  }

  function setField<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((prev) => {
      const next = { ...prev, [key]: value }
      // Reset department (and everything scoped under it) when branch changes — the department
      // dropdown is scoped to the selected branch, so a previously picked department may no
      // longer be offered there.
      if (key === "branchId") {
        next.departmentId = ""
        next.designationId = ""
        next.managerId = ""
      }
      // Reset designation and reporting manager when dept changes — the manager list is
      // scoped to the selected department, so a previously picked manager may no longer apply.
      if (key === "departmentId") {
        next.designationId = ""
        next.managerId = ""
      }
      return next
    })
  }

  // ── Family member row helpers ─────────────────────────────────────────────

  function addFamilyRow() {
    setForm((prev) => ({ ...prev, familyMembers: [...prev.familyMembers, emptyFamilyRow()] }))
  }
  function updateFamilyRow(index: number, patch: Partial<FamilyMemberRow>) {
    setForm((prev) => {
      const rows = [...prev.familyMembers]
      rows[index] = { ...rows[index], ...patch }
      return { ...prev, familyMembers: rows }
    })
  }
  function removeFamilyRow(index: number) {
    setForm((prev) => ({ ...prev, familyMembers: prev.familyMembers.filter((_, i) => i !== index) }))
  }

  // ── Nominee row helpers ────────────────────────────────────────────────────

  function addNomineeRow() {
    setForm((prev) => ({ ...prev, nominees: [...prev.nominees, emptyNomineeRow()] }))
  }
  function updateNomineeRow(index: number, patch: Partial<NomineeRow>) {
    setForm((prev) => {
      const rows = [...prev.nominees]
      let nextPatch: Partial<NomineeRow> = patch
      // Minor status is always derived from DOB, never set by hand — whenever DOB changes,
      // recompute it and clear stale guardian details if the nominee turns out not to be a minor.
      if ("dateOfBirth" in patch) {
        const minor = patch.dateOfBirth ? isMinorAge(patch.dateOfBirth) : false
        nextPatch = {
          ...patch,
          minor,
          ...(minor ? {} : { guardianName: "", guardianRelationship: "" }),
        }
      }
      rows[index] = { ...rows[index], ...nextPatch }
      return { ...prev, nominees: rows }
    })
  }
  function removeNomineeRow(index: number) {
    setForm((prev) => ({ ...prev, nominees: prev.nominees.filter((_, i) => i !== index) }))
  }

  // ── Validation ──────────────────────────────────────────────────────────────

  const ageInvalid = !!(form.dateOfBirth && computeAge(form.dateOfBirth) !== null && computeAge(form.dateOfBirth)! < MIN_AGE)
  const panInvalid    = !!form.panNumber && !PAN_REGEX.test(form.panNumber)
  const aadharInvalid = !!form.aadharNumber && !AADHAR_REGEX.test(form.aadharNumber)
  const ifscInvalid   = !!form.bankIfscCode && !IFSC_REGEX.test(form.bankIfscCode)
  const emailInvalid           = !!form.email.trim() && !EMAIL_REGEX.test(form.email.trim())
  const phoneInvalid           = !!form.phone && !PHONE_REGEX.test(form.phone)
  const pincodeInvalid         = !!form.pincode && !PINCODE_REGEX.test(form.pincode)
  const emergencyPhoneInvalid  = !!form.emergencyContactPhone && !PHONE_REGEX.test(form.emergencyContactPhone)

  const basicRequiredMissing = firstMissingBasicField(form) !== null
  const basicTabHasError = (basicAttempted && basicRequiredMissing) || ageInvalid || emailInvalid || phoneInvalid || pincodeInvalid
  const documentsTabHasError = panInvalid || aadharInvalid || ifscInvalid || emergencyPhoneInvalid
  const familyTabHasError = form.familyMembers.some((r) => !!familyRowError(r))
  const nomineeRowsHaveErrors = form.nominees.some((r) => !!nomineeRowError(r))
  const activeNomineeCount = form.nominees.filter((r) => !isNomineeRowBlank(r)).length
  // Each nominee's own share is validated in nomineeRowFieldErrors (must be 0–100) — nominees are
  // not required to collectively add up to exactly 100, so no aggregate total check here.
  const nomineeTotal = nomineeShareTotal(form.nominees)
  const nomineesTabHasError = nomineeRowsHaveErrors

  function tabIndex(tab: TabKey): number {
    return TAB_ORDER.indexOf(tab)
  }

  function goPrevious() {
    const idx = tabIndex(activeTab)
    if (idx > 0) setActiveTab(TAB_ORDER[idx - 1])
  }

  function goNext() {
    const idx = tabIndex(activeTab)

    if (activeTab === "basic") {
      setBasicAttempted(true)
      const missing = firstMissingBasicField(form)
      if (missing)            { toast.error(missing); return }
      if (ageInvalid)         { toast.error(`Employee must be at least ${MIN_AGE} years old (current age: ${computeAge(form.dateOfBirth)})`); return }
      if (emailInvalid)       { toast.error("Enter a valid email address"); return }
      if (phoneInvalid)       { toast.error("Phone number must be a valid 10-digit Indian mobile number"); return }
      if (pincodeInvalid)     { toast.error("Pincode must be a valid 6-digit Indian PIN code"); return }
    } else if (activeTab === "documents") {
      if (documentsTabHasError) {
        if (panInvalid)              toast.error("PAN must be in format ABCDE1234F")
        else if (aadharInvalid)      toast.error("Aadhar number must be exactly 12 digits")
        else if (ifscInvalid)        toast.error("IFSC code must be in format ABCD0123456")
        else                         toast.error("Emergency contact phone must be a valid 10-digit Indian mobile number")
        return
      }
    } else if (activeTab === "family") {
      if (familyTabHasError) {
        const firstError = form.familyMembers.map(familyRowError).find(Boolean)
        toast.error(firstError || "Please fix the family member details")
        return
      }
    }

    const nextIdx = idx + 1
    if (nextIdx < TAB_ORDER.length) {
      setMaxReachedIndex((m) => Math.max(m, nextIdx))
      setActiveTab(TAB_ORDER[nextIdx])
    }
  }

  function saveNominees() {
    if (nomineesTabHasError) {
      const firstError = form.nominees.map(nomineeRowError).find(Boolean)
      toast.error(firstError || "Please fix the nominee details")
      return
    }
    toast.success("Nominees look good")
  }

  function handleFormKeyDown(e: React.KeyboardEvent<HTMLFormElement>) {
    if (e.key !== "Enter") return
    if (activeTab !== "nominees") {
      e.preventDefault()
      goNext()
    }
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()

    // Once a submit is attempted, every tab becomes freely reachable so the user can jump
    // straight to whichever one has the problem.
    setMaxReachedIndex(TAB_ORDER.length - 1)

    if (basicTabHasError) {
      setActiveTab("basic")
      setBasicAttempted(true)
      const missing = firstMissingBasicField(form)
      if (missing)            toast.error(missing)
      else if (ageInvalid)    toast.error(`Employee must be at least ${MIN_AGE} years old (current age: ${computeAge(form.dateOfBirth)})`)
      else if (emailInvalid)  toast.error("Enter a valid email address")
      else if (phoneInvalid)  toast.error("Phone number must be a valid 10-digit Indian mobile number")
      else                    toast.error("Pincode must be a valid 6-digit Indian PIN code")
      return
    }
    if (documentsTabHasError) {
      setActiveTab("documents")
      if (panInvalid)              toast.error("PAN must be in format ABCDE1234F")
      else if (aadharInvalid)      toast.error("Aadhar number must be exactly 12 digits")
      else if (ifscInvalid)        toast.error("IFSC code must be in format ABCD0123456")
      else                         toast.error("Emergency contact phone must be a valid 10-digit Indian mobile number")
      return
    }
    if (familyTabHasError) {
      setActiveTab("family")
      const firstError = form.familyMembers.map(familyRowError).find(Boolean)
      toast.error(firstError || "Please fix the family member details")
      return
    }
    if (nomineesTabHasError) {
      setActiveTab("nominees")
      const firstError = form.nominees.map(nomineeRowError).find(Boolean)
      toast.error(firstError || "Please fix the nominee details")
      return
    }

    setSubmitting(true)

    const familyMembers: FamilyMemberRequest[] = form.familyMembers
      .filter((r) => !isFamilyRowBlank(r))
      .map((r) => ({
        name: r.name,
        relationship: r.relationship,
        dateOfBirth: r.dateOfBirth || undefined,
        gender: r.gender || undefined,
        occupation: r.occupation || undefined,
        contactNumber: r.contactNumber || undefined,
      }))

    const nominees: NomineeRequest[] = form.nominees
      .filter((r) => !isNomineeRowBlank(r))
      .map((r) => ({
        name: r.name,
        relationship: r.relationship,
        dateOfBirth: r.dateOfBirth || undefined,
        sharePercentage: parseFloat(r.sharePercentage),
        address: r.address || undefined,
        contactNumber: r.contactNumber || undefined,
        minor: r.minor,
        guardianName: r.minor ? (r.guardianName || undefined) : undefined,
        guardianRelationship: r.minor ? (r.guardianRelationship || undefined) : undefined,
      }))

    const payload: EmployeeRequest = {
      firstName:        form.firstName,
      lastName:         form.lastName,
      email:            form.email,
      phone:            form.phone || undefined,
      dateOfBirth:      form.dateOfBirth || undefined,
      departmentId:     form.departmentId,
      designationId:    form.designationId,
      branchId:         form.branchId,
      managerId:        form.managerId || undefined,
      shiftId:          form.shiftId || undefined,
      joiningDate:      form.joiningDate,
      resignationDate:  form.resignationDate || undefined,
      employmentType:   form.employmentType as string,
      employmentStatus: form.employmentStatus as string,
      gender:           form.gender as string,
      address:          form.address || undefined,
      city:             form.city || undefined,
      state:            form.state || undefined,
      pincode:          form.pincode || undefined,
      profilePictureUrl: form.profilePictureUrl || undefined,
      panNumber:        form.panNumber || undefined,
      aadharNumber:     form.aadharNumber || undefined,
      bankAccountNumber: form.bankAccountNumber || undefined,
      bankIfscCode:     form.bankIfscCode || undefined,
      bankName:         form.bankName || undefined,
      emergencyContactName:     form.emergencyContactName || undefined,
      emergencyContactPhone:    form.emergencyContactPhone || undefined,
      emergencyContactRelation: form.emergencyContactRelation || undefined,
      familyMembers,
      nominees,
    }

    try {
      if (editTarget) {
        await employeeService.update(editTarget.id, payload)
        toast.success("Employee updated successfully")
      } else {
        await employeeService.create(payload)
        toast.success("Employee created successfully")
      }
      setDialogOpen(false)
      setPage(0)
      fetchEmployees()
    } catch (err) {
      toastApiError(err, editTarget ? "Failed to update employee" : "Failed to create employee")
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return
    setDeleting(true)
    try {
      await employeeService.delete(deleteTarget.id)
      toast.success("Employee deleted successfully")
      setDeleteTarget(null)
      setPage(0)
      fetchEmployees()
    } catch (err) {
      toastApiError(err, "Failed to delete employee")
    } finally {
      setDeleting(false)
    }
  }

  // ── Render ──────────────────────────────────────────────────────────────────

  return (
    <div className="space-y-4">
      <Toaster position="top-right" />

      <PageHeader
        title="Employees"
        description="Manage your organisation's employees"
        action={
          isHrAdmin ? (
            <Button onClick={openCreate}>
              <Plus className="mr-2 h-4 w-4" />
              Add Employee
            </Button>
          ) : undefined
        }
      />

      {/* Filters */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <Input
          className="sm:max-w-xs"
          placeholder="Search by name, code or email…"
          value={searchInput}
          onChange={(e) => handleSearchInput(e.target.value)}
        />

        <Select
          value={branchFilter}
          onValueChange={(v) => {
            const next = v === "__all__" ? "" : v
            setBranchFilter(next)
            // The department filter's options are scoped to the branch — drop a now-irrelevant pick.
            setDepartmentFilter("")
            setPage(0)
          }}
        >
          <SelectTrigger className="sm:w-48">
            <SelectValue placeholder="All Branches" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="__all__">All Branches</SelectItem>
            {branches.map((b) => (
              <SelectItem key={b.id} value={b.id}>{b.name}</SelectItem>
            ))}
          </SelectContent>
        </Select>

        <Select
          value={departmentFilter}
          onValueChange={(v) => { setDepartmentFilter(v === "__all__" ? "" : v); setPage(0) }}
        >
          <SelectTrigger className="sm:w-48">
            <SelectValue placeholder="All Departments" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="__all__">All Departments</SelectItem>
            {filterDepartments.map((d) => (
              <SelectItem key={d.id} value={d.id}>{d.name}</SelectItem>
            ))}
          </SelectContent>
        </Select>

        <Select
          value={statusFilter}
          onValueChange={(v) => { setStatusFilter(v === "__all__" ? "" : v); setPage(0) }}
        >
          <SelectTrigger className="sm:w-44">
            <SelectValue placeholder="All Statuses" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="__all__">All Statuses</SelectItem>
            <SelectItem value="ACTIVE">Active</SelectItem>
            <SelectItem value="INACTIVE">Inactive</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {/* Table */}
      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Code</TableHead>
              <TableHead>Name</TableHead>
              <TableHead>Department</TableHead>
              <TableHead>Designation</TableHead>
              <TableHead>Branch</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Type</TableHead>
              {isHrAdmin && <TableHead className="text-right">Actions</TableHead>}
            </TableRow>
          </TableHeader>
          <TableBody>
            {loading ? (
              Array.from({ length: 6 }).map((_, i) => (
                <TableRow key={i}>
                  {Array.from({ length: 8 }).map((__, j) => (
                    <TableCell key={j}><Skeleton className="h-4 w-full" /></TableCell>
                  ))}
                </TableRow>
              ))
            ) : employees.length === 0 ? (
              <TableRow>
                <TableCell colSpan={isHrAdmin ? 8 : 7} className="p-0">
                  <EmptyState
                    title="No employees found"
                    description="Try adjusting your filters or add a new employee."
                    action={
                      isHrAdmin ? (
                        <Button onClick={openCreate}>
                          <Plus className="mr-2 h-4 w-4" />
                          Add Employee
                        </Button>
                      ) : undefined
                    }
                  />
                </TableCell>
              </TableRow>
            ) : (
              employees.map((emp) => (
                <TableRow key={emp.id}>
                  <TableCell className="font-mono text-xs">{emp.employeeCode}</TableCell>
                  <TableCell className="font-medium">
                    {emp.firstName} {emp.lastName}
                  </TableCell>
                  <TableCell>{emp.departmentName ?? "—"}</TableCell>
                  <TableCell>{emp.designationTitle ?? "—"}</TableCell>
                  <TableCell>{emp.branchName ?? "—"}</TableCell>
                  <TableCell>
                    <Badge variant={statusBadgeVariant(emp.employmentStatus)}>
                      {emp.employmentStatus.replace("_", " ")}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {emp.employmentType.replace("_", " ")}
                  </TableCell>
                  <TableCell className="text-right">
                    {isHrAdmin && (
                      <div className="flex justify-end gap-1">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => openEdit(emp)}
                          disabled={loadingEdit}
                          aria-label="Edit employee"
                        >
                          <Pencil className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setDeleteTarget(emp)}
                          aria-label="Delete employee"
                          className="text-destructive hover:text-destructive"
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    )}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>

        {!loading && employees.length > 0 && (
          <Pagination
            page={page}
            totalPages={totalPages}
            totalElements={totalElements}
            size={PAGE_SIZE}
            onPageChange={setPage}
          />
        )}
      </div>

      {/* Create / Edit Dialog */}
      <Dialog open={dialogOpen} onOpenChange={handleDialogOpenChange}>
        <DialogContent
          className="max-w-3xl max-h-[90vh] overflow-y-auto"
          onPointerDownOutside={(e) => e.preventDefault()}
        >
          <DialogHeader>
            <DialogTitle>{editTarget ? "Edit Employee" : "Add Employee"}</DialogTitle>
          </DialogHeader>

          <form onSubmit={handleSubmit} onKeyDown={handleFormKeyDown} className="space-y-4">
            <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as TabKey)}>
              <TabsList>
                <TabsTrigger value="basic" hasError={basicTabHasError} disabled={tabIndex("basic") > maxReachedIndex}>Basic Info</TabsTrigger>
                <TabsTrigger value="documents" hasError={documentsTabHasError} disabled={tabIndex("documents") > maxReachedIndex}>Documents &amp; Bank</TabsTrigger>
                <TabsTrigger value="family" hasError={familyTabHasError} disabled={tabIndex("family") > maxReachedIndex}>Family</TabsTrigger>
                <TabsTrigger value="nominees" hasError={nomineesTabHasError} disabled={tabIndex("nominees") > maxReachedIndex}>Nominees</TabsTrigger>
              </TabsList>

              {/* ── Basic Info tab ─────────────────────────────────────────── */}
              <TabsContent value="basic">
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <Label htmlFor="firstName">First Name <span className="text-destructive">*</span></Label>
                    <Input
                      id="firstName"
                      value={form.firstName}
                      onChange={(e) => setField("firstName", e.target.value)}
                      required
                    />
                  </div>
                  <div className="space-y-1">
                    <Label htmlFor="lastName">Last Name <span className="text-destructive">*</span></Label>
                    <Input
                      id="lastName"
                      value={form.lastName}
                      onChange={(e) => setField("lastName", e.target.value)}
                      required
                    />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <Label htmlFor="email">Email <span className="text-destructive">*</span></Label>
                    <Input
                      id="email"
                      type="email"
                      value={form.email}
                      onChange={(e) => setField("email", e.target.value)}
                      required
                    />
                    {emailInvalid && <p className="text-xs text-destructive">Enter a valid email address</p>}
                  </div>
                  <div className="space-y-1">
                    <Label htmlFor="phone">Phone <span className="text-destructive">*</span></Label>
                    <Input
                      id="phone"
                      value={form.phone}
                      placeholder="9876543210"
                      onChange={(e) => setField("phone", normalizePhone(e.target.value))}
                    />
                    {phoneInvalid && <p className="text-xs text-destructive">Must be a valid 10-digit Indian mobile number</p>}
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <Label htmlFor="dateOfBirth">Date of Birth <span className="text-destructive">*</span></Label>
                    <Input
                      id="dateOfBirth"
                      type="date"
                      value={form.dateOfBirth}
                      max={maxDobForAge(MIN_AGE)}
                      onChange={(e) => setField("dateOfBirth", e.target.value)}
                    />
                    {ageInvalid && (
                      <p className="text-xs text-destructive">Must be at least {MIN_AGE} years old</p>
                    )}
                  </div>
                  <div className="space-y-1">
                    <Label>Age</Label>
                    <Input
                      value={form.dateOfBirth ? (computeAge(form.dateOfBirth) !== null ? `${computeAge(form.dateOfBirth)} years` : "") : ""}
                      readOnly
                      disabled
                      placeholder="Auto-calculated"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1">
                  <Label>Branch <span className="text-destructive">*</span></Label>
                  <Select
                    value={form.branchId}
                    onValueChange={(v) => setField("branchId", v)}
                    required
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select branch" />
                    </SelectTrigger>
                    <SelectContent>
                      {branches.map((b) => (
                        <SelectItem key={b.id} value={b.id}>{b.name}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  </div>
                  <div className="space-y-1">
                  <Label>Department <span className="text-destructive">*</span></Label>
                  <Select
                    value={form.departmentId}
                    onValueChange={(v) => setField("departmentId", v)}
                    disabled={!form.branchId}
                    required
                  >
                    <SelectTrigger>
                      <SelectValue placeholder={form.branchId ? "Select department" : "Select branch first"} />
                    </SelectTrigger>
                    <SelectContent>
                      {dialogDepartments.length === 0 && form.branchId ? (
                        <div className="px-2 py-1.5 text-sm text-muted-foreground">
                          No departments assigned to this branch yet
                        </div>
                      ) : (
                        dialogDepartments.map((d) => (
                          <SelectItem key={d.id} value={d.id}>{d.name}</SelectItem>
                        ))
                      )}
                    </SelectContent>
                  </Select>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1">
                     <Label>Designation <span className="text-destructive">*</span></Label>
                    <Select
                      value={form.designationId}
                      onValueChange={(v) => setField("designationId", v)}
                      disabled={!form.departmentId}
                      required
                    >
                      <SelectTrigger>
                        <SelectValue placeholder={form.departmentId ? "Select designation" : "Select dept first"} />
                      </SelectTrigger>
                      <SelectContent>
                        {dialogDesignations.map((d) => (
                          <SelectItem key={d.id} value={d.id}>{d.name}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="space-y-1">
                    <Label>Reporting Manager</Label>
                    <Combobox
                      options={[
                        { value: "", label: "No manager" },
                        ...empSummaries
                          .filter((e) => !editTarget || e.id !== editTarget.id)
                          .filter((e) => e.departmentId === form.departmentId)
                          .filter((e) => e.managerial === true)
                          .map((e) => ({ value: e.id, label: `${e.fullName} (${e.employeeCode})` })),
                      ]}
                      value={form.managerId}
                      onChange={(v) => setField("managerId", v)}
                      placeholder={form.departmentId ? "No manager" : "Select dept first"}
                      searchPlaceholder="Search employees…"
                      disabled={!form.departmentId}
                    />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <Label>Shift</Label>
                    <Select
                      value={form.shiftId}
                      onValueChange={(v) => setField("shiftId", v === "__none__" ? "" : v)}
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="No shift assigned" />
                      </SelectTrigger>
                      <SelectContent>
                       {/* <SelectItem value="__none__">No shift assigned</SelectItem> */}
                        {shifts.filter((s) => s.active).map((s) => (
                          <SelectItem key={s.id} value={s.id}>
                            {s.name} ({s.startTime}–{s.endTime})
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <Label htmlFor="joiningDate">Joining Date <span className="text-destructive">*</span></Label>
                    <Input
                      id="joiningDate"
                      type="date"
                      value={form.joiningDate}
                      onChange={(e) => setField("joiningDate", e.target.value)}
                      required
                    />
                  </div>
                  <div className="space-y-1">
                    <Label>Gender <span className="text-destructive">*</span></Label>
                    <Select
                      value={form.gender}
                      onValueChange={(v) => setField("gender", v as Gender)}
                      required
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="Select gender" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="MALE">Male</SelectItem>
                        <SelectItem value="FEMALE">Female</SelectItem>
                        <SelectItem value="OTHER">Other</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <Label>Employment Type <span className="text-destructive">*</span></Label>
                    <Select
                      value={form.employmentType}
                      onValueChange={(v) => setField("employmentType", v as EmploymentType)}
                      required
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="Select type" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="FULL_TIME">Full Time</SelectItem>
                        <SelectItem value="PART_TIME">Part Time</SelectItem>
                        <SelectItem value="CONTRACT">Contract</SelectItem>
                        <SelectItem value="INTERN">Intern</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="space-y-1">
                    <Label>Employment Status <span className="text-destructive">*</span></Label>
                    <Select
                      value={form.employmentStatus}
                      onValueChange={(v) => setField("employmentStatus", v as EmploymentStatus)}
                      required
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="Select status" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="ACTIVE">Active</SelectItem>
                        <SelectItem value="INACTIVE">Inactive</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                </div>

                {form.employmentStatus === "INACTIVE" && (
                  <div className="space-y-1">
                    <Label htmlFor="resignationDate">Resignation Date</Label>
                    <Input
                      id="resignationDate"
                      type="date"
                      value={form.resignationDate}
                      onChange={(e) => setField("resignationDate", e.target.value)}
                    />
                  </div>
                )}

                <div className="space-y-1">
                  <Label htmlFor="address">Address</Label>
                  <Input
                    id="address"
                    value={form.address}
                    onChange={(e) => setField("address", e.target.value)}
                  />
                </div>

                <div className="grid grid-cols-3 gap-4">
                  <div className="space-y-1">
                    <Label htmlFor="city">City</Label>
                    <Input
                      id="city"
                      value={form.city}
                      onChange={(e) => setField("city", e.target.value)}
                    />
                  </div>
                  <div className="space-y-1">
                    <Label htmlFor="state">State</Label>
                    <Input
                      id="state"
                      value={form.state}
                      onChange={(e) => setField("state", e.target.value)}
                    />
                  </div>
                  <div className="space-y-1">
                    <Label htmlFor="pincode">Pincode</Label>
                    <Input
                      id="pincode"
                      value={form.pincode}
                      placeholder="560001"
                      maxLength={6}
                      onChange={(e) => setField("pincode", e.target.value.replace(/\D/g, ""))}
                    />
                    {pincodeInvalid && <p className="text-xs text-destructive">Must be a valid 6-digit Indian PIN code</p>}
                  </div>
                </div>

                <div className="space-y-1">
                  <Label htmlFor="profilePictureUrl">Profile Picture URL</Label>
                  <Input
                    id="profilePictureUrl"
                    value={form.profilePictureUrl}
                    onChange={(e) => setField("profilePictureUrl", e.target.value)}
                  />
                </div>
              </TabsContent>

              {/* ── Documents & Bank tab ───────────────────────────────────── */}
              <TabsContent value="documents">
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <Label htmlFor="panNumber">PAN Number</Label>
                    <Input
                      id="panNumber"
                      value={form.panNumber}
                      placeholder="ABCDE1234F"
                      maxLength={10}
                      onChange={(e) => setField("panNumber", e.target.value.toUpperCase())}
                    />
                    {panInvalid && <p className="text-xs text-destructive">Format: ABCDE1234F</p>}
                  </div>
                  <div className="space-y-1">
                    <Label htmlFor="aadharNumber">Aadhar Number</Label>
                    <Input
                      id="aadharNumber"
                      value={form.aadharNumber}
                      placeholder="123456789012"
                      onChange={(e) => setField("aadharNumber", e.target.value.replace(/\D/g, ""))}
                      maxLength={12}
                    />
                    {aadharInvalid && <p className="text-xs text-destructive">Must be exactly 12 digits</p>}
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <Label htmlFor="bankAccountNumber">Bank Account Number</Label>
                    <Input
                      id="bankAccountNumber"
                      value={form.bankAccountNumber}
                      onChange={(e) => setField("bankAccountNumber", e.target.value)}
                    />
                  </div>
                  <div className="space-y-1">
                    <Label htmlFor="bankIfscCode">IFSC Code</Label>
                    <Input
                      id="bankIfscCode"
                      value={form.bankIfscCode}
                      placeholder="ABCD0123456"
                      maxLength={11}
                      onChange={(e) => setField("bankIfscCode", e.target.value.toUpperCase())}
                    />
                    {ifscInvalid && <p className="text-xs text-destructive">Format: ABCD0123456</p>}
                  </div>
                </div>

                <div className="space-y-1">
                  <Label htmlFor="bankName">Bank Name</Label>
                  <Input
                    id="bankName"
                    value={form.bankName}
                    onChange={(e) => setField("bankName", e.target.value)}
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <Label htmlFor="emergencyContactName">Emergency Contact Name</Label>
                    <Input
                      id="emergencyContactName"
                      value={form.emergencyContactName}
                      onChange={(e) => setField("emergencyContactName", e.target.value)}
                    />
                  </div>
                  <div className="space-y-1">
                    <Label htmlFor="emergencyContactPhone">Emergency Contact Phone</Label>
                    <Input
                      id="emergencyContactPhone"
                      value={form.emergencyContactPhone}
                      placeholder="9876543210"
                      onChange={(e) => setField("emergencyContactPhone", normalizePhone(e.target.value))}
                    />
                    {emergencyPhoneInvalid && <p className="text-xs text-destructive">Must be a valid 10-digit Indian mobile number</p>}
                  </div>
                </div>

                <div className="space-y-1">
                  <Label htmlFor="emergencyContactRelation">Emergency Contact Relation</Label>
                  <Input
                    id="emergencyContactRelation"
                    value={form.emergencyContactRelation}
                    onChange={(e) => setField("emergencyContactRelation", e.target.value)}
                  />
                </div>
              </TabsContent>

              {/* ── Family tab ─────────────────────────────────────────────── */}
              <TabsContent value="family">
                {form.familyMembers.length === 0 && (
                  <p className="text-sm text-muted-foreground">No family members added yet.</p>
                )}

                {form.familyMembers.map((row, index) => {
                  const fieldErrors = familyRowFieldErrors(row)
                  return (
                    <div key={row.key} className="rounded-md border p-3 space-y-3">
                      <div className="flex items-center justify-between">
                        <span className="text-sm font-medium">Family Member {index + 1}</span>
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          onClick={() => removeFamilyRow(index)}
                          aria-label="Remove family member"
                        >
                          <X className="h-4 w-4" />
                        </Button>
                      </div>

                      <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-1">
                          <Label>Name</Label>
                          <Input
                            value={row.name}
                            aria-invalid={!!fieldErrors.name}
                            className={fieldErrors.name ? "border-destructive" : undefined}
                            onChange={(e) => updateFamilyRow(index, { name: e.target.value })}
                          />
                          {fieldErrors.name && <p className="text-xs text-destructive">{fieldErrors.name}</p>}
                        </div>
                        <div className="space-y-1">
                          <Label>Relationship</Label>
                          <Select
                            value={row.relationship}
                            onValueChange={(v) => updateFamilyRow(index, { relationship: v as Relationship })}
                          >
                            <SelectTrigger
                              aria-invalid={!!fieldErrors.relationship}
                              className={fieldErrors.relationship ? "border-destructive" : undefined}
                            >
                              <SelectValue placeholder="Select relationship" />
                            </SelectTrigger>
                            <SelectContent>
                              {RELATIONSHIP_OPTIONS.map((o) => (
                                <SelectItem key={o.value} value={o.value}>{o.label}</SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                          {fieldErrors.relationship && <p className="text-xs text-destructive">{fieldErrors.relationship}</p>}
                        </div>
                      </div>

                      <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-1">
                          <Label>Date of Birth</Label>
                          <Input
                            type="date"
                            value={row.dateOfBirth}
                            max={todayStr()}
                            aria-invalid={!!fieldErrors.dateOfBirth}
                            className={fieldErrors.dateOfBirth ? "border-destructive" : undefined}
                            onChange={(e) => updateFamilyRow(index, { dateOfBirth: e.target.value })}
                          />
                          {fieldErrors.dateOfBirth && <p className="text-xs text-destructive">{fieldErrors.dateOfBirth}</p>}
                        </div>
                        <div className="space-y-1">
                          <Label>Gender</Label>
                          <Select
                            value={row.gender}
                            onValueChange={(v) => updateFamilyRow(index, { gender: v as Gender })}
                          >
                            <SelectTrigger>
                              <SelectValue placeholder="Select gender" />
                            </SelectTrigger>
                            <SelectContent>
                              <SelectItem value="MALE">Male</SelectItem>
                              <SelectItem value="FEMALE">Female</SelectItem>
                              <SelectItem value="OTHER">Other</SelectItem>
                            </SelectContent>
                          </Select>
                        </div>
                      </div>

                      <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-1">
                          <Label>Occupation</Label>
                          <Input
                            value={row.occupation}
                            onChange={(e) => updateFamilyRow(index, { occupation: e.target.value })}
                          />
                        </div>
                        <div className="space-y-1">
                          <Label>Contact Number</Label>
                          <Input
                            value={row.contactNumber}
                            placeholder="9876543210"
                            aria-invalid={!!fieldErrors.contactNumber}
                            className={fieldErrors.contactNumber ? "border-destructive" : undefined}
                            onChange={(e) => updateFamilyRow(index, { contactNumber: normalizePhone(e.target.value) })}
                          />
                          {fieldErrors.contactNumber && <p className="text-xs text-destructive">{fieldErrors.contactNumber}</p>}
                        </div>
                      </div>
                    </div>
                  )
                })}

                <Button type="button" variant="outline" onClick={addFamilyRow}>
                  <Plus className="mr-2 h-4 w-4" />
                  Add Family Member
                </Button>
              </TabsContent>

              {/* ── Nominees tab ───────────────────────────────────────────── */}
              <TabsContent value="nominees">
                {form.nominees.length === 0 && (
                  <p className="text-sm text-muted-foreground">No nominees added yet.</p>
                )}

                {form.nominees.map((row, index) => {
                  const fieldErrors = nomineeRowFieldErrors(row)
                  return (
                    <div key={row.key} className="rounded-md border p-3 space-y-3">
                      <div className="flex items-center justify-between">
                        <span className="text-sm font-medium">Nominee {index + 1}</span>
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          onClick={() => removeNomineeRow(index)}
                          aria-label="Remove nominee"
                        >
                          <X className="h-4 w-4" />
                        </Button>
                      </div>

                      <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-1">
                          <Label>Name</Label>
                          <Input
                            value={row.name}
                            aria-invalid={!!fieldErrors.name}
                            className={fieldErrors.name ? "border-destructive" : undefined}
                            onChange={(e) => updateNomineeRow(index, { name: e.target.value })}
                          />
                          {fieldErrors.name && <p className="text-xs text-destructive">{fieldErrors.name}</p>}
                        </div>
                        <div className="space-y-1">
                          <Label>Relationship</Label>
                          <Select
                            value={row.relationship}
                            onValueChange={(v) => updateNomineeRow(index, { relationship: v as Relationship })}
                          >
                            <SelectTrigger
                              aria-invalid={!!fieldErrors.relationship}
                              className={fieldErrors.relationship ? "border-destructive" : undefined}
                            >
                              <SelectValue placeholder="Select relationship" />
                            </SelectTrigger>
                            <SelectContent>
                              {RELATIONSHIP_OPTIONS.map((o) => (
                                <SelectItem key={o.value} value={o.value}>{o.label}</SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                          {fieldErrors.relationship && <p className="text-xs text-destructive">{fieldErrors.relationship}</p>}
                        </div>
                      </div>

                      <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-1">
                          <Label>Date of Birth</Label>
                          <Input
                            type="date"
                            value={row.dateOfBirth}
                            max={todayStr()}
                            aria-invalid={!!fieldErrors.dateOfBirth}
                            className={fieldErrors.dateOfBirth ? "border-destructive" : undefined}
                            onChange={(e) => updateNomineeRow(index, { dateOfBirth: e.target.value })}
                          />
                          {fieldErrors.dateOfBirth && <p className="text-xs text-destructive">{fieldErrors.dateOfBirth}</p>}
                        </div>
                        <div className="space-y-1">
                          <Label>Share Percentage</Label>
                          <Input
                            type="number"
                            min={0}
                            max={100}
                            step="0.01"
                            value={row.sharePercentage}
                            aria-invalid={!!fieldErrors.sharePercentage}
                            className={fieldErrors.sharePercentage ? "border-destructive" : undefined}
                            onChange={(e) => updateNomineeRow(index, { sharePercentage: e.target.value })}
                          />
                          {fieldErrors.sharePercentage && <p className="text-xs text-destructive">{fieldErrors.sharePercentage}</p>}
                        </div>
                      </div>

                      <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-1">
                          <Label>Address</Label>
                          <Input
                            value={row.address}
                            onChange={(e) => updateNomineeRow(index, { address: e.target.value })}
                          />
                        </div>
                        <div className="space-y-1">
                          <Label>Contact Number</Label>
                          <Input
                            value={row.contactNumber}
                            placeholder="9876543210"
                            aria-invalid={!!fieldErrors.contactNumber}
                            className={fieldErrors.contactNumber ? "border-destructive" : undefined}
                            onChange={(e) => updateNomineeRow(index, { contactNumber: normalizePhone(e.target.value) })}
                          />
                          {fieldErrors.contactNumber && <p className="text-xs text-destructive">{fieldErrors.contactNumber}</p>}
                        </div>
                      </div>

                      <div className="flex items-center gap-2">
                        <Checkbox
                          id={`minor-${row.key}`}
                          checked={row.minor}
                          disabled
                        />
                        <Label htmlFor={`minor-${row.key}`}>Nominee is a minor</Label>
                        <span className="text-xs text-muted-foreground">
                          {row.dateOfBirth
                            ? "(auto-detected from date of birth)"
                            : "(enter date of birth to determine automatically)"}
                        </span>
                      </div>

                      {row.minor && (
                        <div className="grid grid-cols-2 gap-4">
                          <div className="space-y-1">
                            <Label>Guardian Name</Label>
                            <Input
                              value={row.guardianName}
                              aria-invalid={!!fieldErrors.guardianName}
                              className={fieldErrors.guardianName ? "border-destructive" : undefined}
                              onChange={(e) => updateNomineeRow(index, { guardianName: e.target.value })}
                            />
                            {fieldErrors.guardianName && <p className="text-xs text-destructive">{fieldErrors.guardianName}</p>}
                          </div>
                          <div className="space-y-1">
                            <Label>Guardian Relationship</Label>
                            <Select
                              value={row.guardianRelationship}
                              onValueChange={(v) => updateNomineeRow(index, { guardianRelationship: v as Relationship })}
                            >
                              <SelectTrigger
                                aria-invalid={!!fieldErrors.guardianRelationship}
                                className={fieldErrors.guardianRelationship ? "border-destructive" : undefined}
                              >
                                <SelectValue placeholder="Select relationship" />
                              </SelectTrigger>
                              <SelectContent>
                                {RELATIONSHIP_OPTIONS.map((o) => (
                                  <SelectItem key={o.value} value={o.value}>{o.label}</SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                            {fieldErrors.guardianRelationship && <p className="text-xs text-destructive">{fieldErrors.guardianRelationship}</p>}
                          </div>
                        </div>
                      )}
                    </div>
                  )
                })}

                <Button type="button" variant="outline" onClick={addNomineeRow}>
                  <Plus className="mr-2 h-4 w-4" />
                  Add Nominee
                </Button>

                {activeNomineeCount > 0 && (
                  <p className="text-sm text-muted-foreground">
                    Total share allocated: {nomineeTotal}%
                  </p>
                )}
              </TabsContent>
            </Tabs>

            <DialogFooter className="justify-between">
              <div>
                {activeTab !== "basic" && (
                  <Button type="button" variant="outline" onClick={goPrevious} disabled={submitting}>
                    <ChevronLeft className="mr-2 h-4 w-4" />
                    Previous
                  </Button>
                )}
              </div>
              <div className="flex gap-2">
                <DialogClose asChild>
                  <Button type="button" variant="outline" disabled={submitting}>
                    Cancel
                  </Button>
                </DialogClose>
                {activeTab === "nominees" ? (
                  <React.Fragment key="nominees-actions">
                    <Button
                      type="button"
                      variant="outline"
                      disabled={submitting}
                      onClick={(e) => { e.preventDefault(); saveNominees() }}
                    >
                      Save
                    </Button>
                    <Button type="submit" disabled={submitting}>
                      {submitting ? "Saving…" : editTarget ? "Update Employee" : "Create Employee"}
                    </Button>
                  </React.Fragment>
                ) : (
                  <Button key="save-next-button" type="button" onClick={(e) => { e.preventDefault(); goNext() }}>
                    Save and Next
                    <ChevronRight className="ml-2 h-4 w-4" />
                  </Button>
                )}
              </div>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Delete Confirm */}
      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={(open) => { if (!open) setDeleteTarget(null) }}
        title="Delete Employee"
        description={
          deleteTarget
            ? `Are you sure you want to delete ${deleteTarget.firstName} ${deleteTarget.lastName}? This action cannot be undone.`
            : undefined
        }
        confirmLabel="Delete"
        variant="destructive"
        loading={deleting}
        onConfirm={handleDelete}
      />

      {/* Cancel Confirm */}
      <ConfirmDialog
        open={showCancelConfirm}
        onOpenChange={setShowCancelConfirm}
        title="Discard changes?"
        description="You have unsaved changes in this form. Are you sure you want to cancel? Your changes will be lost."
        confirmLabel="Discard"
        cancelLabel="Keep Editing"
        variant="destructive"
        onConfirm={confirmCancel}
      />
    </div>
  )
}

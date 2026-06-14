import React, { useEffect, useRef, useState, useCallback } from "react"
import { Pencil, Trash2, Plus } from "lucide-react"
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
import {
  Table, TableHeader, TableBody, TableRow, TableHead, TableCell,
} from "@/components/ui/table"
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogClose,
} from "@/components/ui/dialog"
import {
  Select, SelectTrigger, SelectValue, SelectContent, SelectItem,
} from "@/components/ui/select"

import employeeService from "@/services/employeeService"
import type { EmployeeFilter, EmployeeRequest, EmployeeResponse } from "@/services/employeeService"
import departmentService from "@/services/departmentService"
import type { DepartmentResponse } from "@/services/departmentService"
import designationService from "@/services/designationService"
import type { DesignationResponse } from "@/services/designationService"
import branchService from "@/services/branchService"
import type { BranchResponse } from "@/services/branchService"
import { toastApiError } from "@/services/api"
import { useIsHrAdmin } from "@/hooks/useRole"

// ── Types ─────────────────────────────────────────────────────────────────────

type EmploymentStatus = "ACTIVE" | "INACTIVE" | "ON_LEAVE" | "TERMINATED"
type EmploymentType   = "FULL_TIME" | "PART_TIME" | "CONTRACT" | "INTERN"
type Gender           = "MALE" | "FEMALE" | "OTHER"

interface FormState {
  firstName: string
  lastName: string
  email: string
  phone: string
  departmentId: string
  designationId: string
  branchId: string
  joiningDate: string
  employmentType: EmploymentType | ""
  employmentStatus: EmploymentStatus | ""
  gender: Gender | ""
}

const EMPTY_FORM: FormState = {
  firstName: "",
  lastName: "",
  email: "",
  phone: "",
  departmentId: "",
  designationId: "",
  branchId: "",
  joiningDate: "",
  employmentType: "",
  employmentStatus: "",
  gender: "",
}

// ── Badge helper ──────────────────────────────────────────────────────────────

function statusBadgeVariant(
  status: string
): "success" | "secondary" | "warning" | "destructive" | "default" {
  switch (status) {
    case "ACTIVE":     return "success"
    case "INACTIVE":   return "secondary"
    case "ON_LEAVE":   return "warning"
    case "TERMINATED": return "destructive"
    default:           return "default"
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
  const [statusFilter, setStatusFilter]     = useState<string>("")

  // Reference data
  const [departments, setDepartments]   = useState<DepartmentResponse[]>([])
  const [designations, setDesignations] = useState<DesignationResponse[]>([])
  const [branches, setBranches]         = useState<BranchResponse[]>([])

  // Dialog state
  const [dialogOpen, setDialogOpen]         = useState(false)
  const [editTarget, setEditTarget]         = useState<EmployeeResponse | null>(null)
  const [form, setForm]                     = useState<FormState>(EMPTY_FORM)
  const [dialogDesignations, setDialogDesignations] = useState<DesignationResponse[]>([])
  const [submitting, setSubmitting]         = useState(false)

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
  }, [page, search, departmentFilter, statusFilter])

  useEffect(() => { fetchEmployees() }, [fetchEmployees])

  // Load reference data once
  useEffect(() => {
    departmentService.getAll().then((r) => {
      const data = (r.data as any).data ?? r.data
      setDepartments(Array.isArray(data) ? data : [])
    }).catch(() => setDepartments([]))

    branchService.getAll().then((r) => {
      const data = (r.data as any).data ?? r.data
      setBranches(Array.isArray(data) ? data : [])
    }).catch(() => setBranches([]))
  }, [])

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
    setDialogDesignations([])
    setDialogOpen(true)
  }

  function openEdit(emp: EmployeeResponse) {
    setEditTarget(emp)
    setForm({
      firstName:        emp.firstName,
      lastName:         emp.lastName,
      email:            emp.email,
      phone:            emp.phone ?? "",
      departmentId:     emp.departmentId,
      designationId:    emp.designationId,
      branchId:         emp.branchId,
      joiningDate:      emp.joiningDate,
      employmentType:   (emp.employmentType as EmploymentType) || "",
      employmentStatus: (emp.employmentStatus as EmploymentStatus) || "",
      gender:           (emp.gender as Gender) || "",
    })
    setDialogOpen(true)
  }

  function setField<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((prev) => {
      const next = { ...prev, [key]: value }
      // Reset designation when dept changes
      if (key === "departmentId") next.designationId = ""
      return next
    })
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setSubmitting(true)

    const payload: EmployeeRequest = {
      firstName:        form.firstName,
      lastName:         form.lastName,
      email:            form.email,
      phone:            form.phone || undefined,
      departmentId:     form.departmentId,
      designationId:    form.designationId,
      branchId:         form.branchId,
      joiningDate:      form.joiningDate,
      employmentType:   form.employmentType as string,
      employmentStatus: form.employmentStatus as string,
      gender:           form.gender as string,
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
          value={departmentFilter}
          onValueChange={(v) => { setDepartmentFilter(v === "__all__" ? "" : v); setPage(0) }}
        >
          <SelectTrigger className="sm:w-48">
            <SelectValue placeholder="All Departments" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="__all__">All Departments</SelectItem>
            {departments.map((d) => (
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
            <SelectItem value="ON_LEAVE">On Leave</SelectItem>
            <SelectItem value="TERMINATED">Terminated</SelectItem>
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
                  <TableCell>{emp.designationName ?? "—"}</TableCell>
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
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{editTarget ? "Edit Employee" : "Add Employee"}</DialogTitle>
          </DialogHeader>

          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Row: First Name / Last Name */}
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

            {/* Row: Email / Phone */}
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
              </div>
              <div className="space-y-1">
                <Label htmlFor="phone">Phone</Label>
                <Input
                  id="phone"
                  value={form.phone}
                  onChange={(e) => setField("phone", e.target.value)}
                />
              </div>
            </div>

            {/* Row: Department / Designation */}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1">
                <Label>Department <span className="text-destructive">*</span></Label>
                <Select
                  value={form.departmentId}
                  onValueChange={(v) => setField("departmentId", v)}
                  required
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select department" />
                  </SelectTrigger>
                  <SelectContent>
                    {departments.map((d) => (
                      <SelectItem key={d.id} value={d.id}>{d.name}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
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
            </div>

            {/* Branch */}
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

            {/* Row: Joining Date / Gender */}
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

            {/* Row: Employment Type / Employment Status */}
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
                    <SelectItem value="ON_LEAVE">On Leave</SelectItem>
                    <SelectItem value="TERMINATED">Terminated</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            <DialogFooter>
              <DialogClose asChild>
                <Button type="button" variant="outline" disabled={submitting}>
                  Cancel
                </Button>
              </DialogClose>
              <Button type="submit" disabled={submitting}>
                {submitting ? "Saving…" : editTarget ? "Update Employee" : "Create Employee"}
              </Button>
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
    </div>
  )
}

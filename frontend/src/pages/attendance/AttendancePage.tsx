import React, { useEffect, useMemo, useState } from "react"
import toast from "react-hot-toast"
import { RefreshCw, CheckCircle2, XCircle, Lock } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
} from "@/components/ui/table"
import PageHeader from "@/components/shared/PageHeader"
import EmptyState from "@/components/shared/EmptyState"

import attendanceService, { type TodayAttendanceRow } from "@/services/attendanceService"
import departmentService, { type DepartmentResponse } from "@/services/departmentService"
import shiftService, { type ShiftResponse } from "@/services/shiftService"
import { toastApiError } from "@/services/api"
import { useAuthStore } from "@/store/authStore"

// ── Helpers ───────────────────────────────────────────────────────────────────

type AttendanceStatus = "PRESENT" | "ABSENT"
const LOCKED_STATUSES = new Set(["ON_LEAVE", "HALF_DAY"])

function statusBadge(status?: string) {
  switch (status) {
    case "PRESENT":
      return <Badge variant="success">Present</Badge>
    case "ABSENT":
      return <Badge variant="destructive">Absent</Badge>
    case "HALF_DAY":
      return <Badge variant="warning">Half Day</Badge>
    case "ON_LEAVE":
      return <Badge variant="warning">On Leave</Badge>
    default:
      return <Badge variant="outline">Not Marked</Badge>
  }
}

function todayLabel() {
  return new Date().toLocaleDateString("en-IN", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
  })
}

// ── Main Page ─────────────────────────────────────────────────────────────────

export default function AttendancePage() {
  const [rows, setRows]       = useState<TodayAttendanceRow[]>([])
  const [loading, setLoading] = useState(true)
  const [marking, setMarking] = useState<string | null>(null)

  const [departments, setDepartments] = useState<DepartmentResponse[]>([])
  const [shifts, setShifts]           = useState<ShiftResponse[]>([])
  const [search, setSearch]                 = useState("")
  const [departmentFilter, setDepartmentFilter] = useState("__all__")
  const [shiftFilter, setShiftFilter]           = useState("__all__")

  const user      = useAuthStore((s) => s.user)
  const isHrAdmin = user?.roles?.includes("ROLE_HR_ADMIN") ?? false

  async function loadToday() {
    setLoading(true)
    try {
      const res  = await attendanceService.getTodayAttendance()
      const data = (res.data as any).data ?? res.data
      setRows(Array.isArray(data) ? data : [])
    } catch (err) {
      toastApiError(err, "Failed to load today's attendance.")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadToday() }, [])

  useEffect(() => {
    departmentService.getAll().then((r) => {
      const data = (r.data as any).data ?? r.data
      setDepartments(Array.isArray(data) ? data : [])
    }).catch(() => setDepartments([]))

    shiftService.getAll().then((r) => {
      const data = (r.data as any).data ?? r.data
      setShifts(Array.isArray(data) ? data : [])
    }).catch(() => setShifts([]))
  }, [])

  const filteredRows = useMemo(() => {
    const q = search.trim().toLowerCase()
    return rows.filter((r) => {
      if (departmentFilter !== "__all__" && r.departmentId !== departmentFilter) return false
      if (shiftFilter !== "__all__" && r.shiftId !== shiftFilter) return false
      if (q) {
        const haystack = `${r.employeeName} ${r.email ?? ""} ${r.employeeCode}`.toLowerCase()
        if (!haystack.includes(q)) return false
      }
      return true
    })
  }, [rows, search, departmentFilter, shiftFilter])

  async function handleMark(employeeId: string, status: AttendanceStatus) {
    setMarking(employeeId + status)
    try {
      const res     = await attendanceService.markAttendance(employeeId, status)
      const updated: TodayAttendanceRow = (res.data as any).data ?? res.data
      setRows((prev) =>
        prev.map((r) => (r.employeeId === employeeId ? { ...r, ...updated } : r))
      )
      toast.success(`Marked ${updated.employeeName} as ${status.replace("_", " ")}`)
    } catch (err) {
      toastApiError(err, "Failed to mark attendance.")
    } finally {
      setMarking(null)
    }
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Attendance"
        description={`Today — ${todayLabel()}`}
        action={
          <Button variant="outline" size="sm" onClick={loadToday} disabled={loading} className="gap-1">
            <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
            Refresh
          </Button>
        }
      />

      <Card>
        <CardContent className="pt-4">
          <div className="flex flex-wrap items-center gap-3">
            <Input
              className="sm:max-w-xs"
              placeholder="Search by name, email or code…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <Select value={departmentFilter} onValueChange={setDepartmentFilter}>
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
            <Select value={shiftFilter} onValueChange={setShiftFilter}>
              <SelectTrigger className="sm:w-48">
                <SelectValue placeholder="All Shifts" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="__all__">All Shifts</SelectItem>
                {shifts.map((s) => (
                  <SelectItem key={s.id} value={s.id}>{s.name}</SelectItem>
                ))}
              </SelectContent>
            </Select>
            {(search || departmentFilter !== "__all__" || shiftFilter !== "__all__") && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => { setSearch(""); setDepartmentFilter("__all__"); setShiftFilter("__all__") }}
              >
                Clear
              </Button>
            )}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="p-0">
          {loading ? (
            <div className="p-6 space-y-3">
              {Array.from({ length: 8 }).map((_, i) => (
                <Skeleton key={i} className="h-10 w-full rounded" />
              ))}
            </div>
          ) : filteredRows.length === 0 ? (
            <EmptyState
              title="No matching employees"
              description="No employees match the selected filters."
            />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Employee</TableHead>
                  <TableHead>Department</TableHead>
                  <TableHead>Designation</TableHead>
                  <TableHead>Shift</TableHead>
                  <TableHead>Status</TableHead>
                  {isHrAdmin && <TableHead className="text-right">Mark Attendance</TableHead>}
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredRows.map((row) => {
                  const busy = marking !== null && marking.startsWith(row.employeeId)
                  const locked = LOCKED_STATUSES.has(row.status ?? "")
                  return (
                    <TableRow key={row.employeeId}>
                      <TableCell>
                        <div className="font-medium text-sm">{row.employeeName}</div>
                        <div className="text-xs text-muted-foreground">{row.employeeCode}</div>
                      </TableCell>
                      <TableCell className="text-sm">{row.departmentName ?? "—"}</TableCell>
                      <TableCell className="text-sm">{row.designationTitle ?? "—"}</TableCell>
                      <TableCell className="text-sm">{row.shiftName ?? "—"}</TableCell>
                      <TableCell>{statusBadge(row.status)}</TableCell>
                      {isHrAdmin && (
                        <TableCell className="text-right">
                          {locked ? (
                            <div className="flex items-center justify-end gap-1 text-xs text-muted-foreground">
                              <Lock className="h-3.5 w-3.5" />
                              Locked
                            </div>
                          ) : (
                            <div className="flex justify-end gap-1">
                              <Button
                                size="sm"
                                variant={row.status === "PRESENT" ? "default" : "outline"}
                                className="gap-1 text-xs"
                                disabled={busy}
                                onClick={() => handleMark(row.employeeId, "PRESENT")}
                              >
                                <CheckCircle2 className="h-3.5 w-3.5" />
                                Present
                              </Button>
                              <Button
                                size="sm"
                                variant={row.status === "ABSENT" ? "destructive" : "outline"}
                                className="gap-1 text-xs"
                                disabled={busy}
                                onClick={() => handleMark(row.employeeId, "ABSENT")}
                              >
                                <XCircle className="h-3.5 w-3.5" />
                                Absent
                              </Button>
                            </div>
                          )}
                        </TableCell>
                      )}
                    </TableRow>
                  )
                })}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

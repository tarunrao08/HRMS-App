import React, { useEffect, useState } from "react"
import toast from "react-hot-toast"
import { RefreshCw, CheckCircle2, XCircle, Clock } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
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
import { toastApiError } from "@/services/api"
import { useAuthStore } from "@/store/authStore"

// ── Helpers ───────────────────────────────────────────────────────────────────

type AttendanceStatus = "PRESENT" | "ABSENT" | "HALF_DAY"

function statusBadge(status?: string) {
  switch (status) {
    case "PRESENT":
      return <Badge variant="success">Present</Badge>
    case "ABSENT":
      return <Badge variant="destructive">Absent</Badge>
    case "HALF_DAY":
      return <Badge variant="warning">Half Day</Badge>
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
        <CardContent className="p-0">
          {loading ? (
            <div className="p-6 space-y-3">
              {Array.from({ length: 8 }).map((_, i) => (
                <Skeleton key={i} className="h-10 w-full rounded" />
              ))}
            </div>
          ) : rows.length === 0 ? (
            <EmptyState
              title="No active employees"
              description="There are no active employees to display."
            />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Employee</TableHead>
                  <TableHead>Department</TableHead>
                  <TableHead>Designation</TableHead>
                  <TableHead>Status</TableHead>
                  {isHrAdmin && <TableHead className="text-right">Mark Attendance</TableHead>}
                </TableRow>
              </TableHeader>
              <TableBody>
                {rows.map((row) => {
                  const busy = marking !== null && marking.startsWith(row.employeeId)
                  return (
                    <TableRow key={row.employeeId}>
                      <TableCell>
                        <div className="font-medium text-sm">{row.employeeName}</div>
                        <div className="text-xs text-muted-foreground">{row.employeeCode}</div>
                      </TableCell>
                      <TableCell className="text-sm">{row.departmentName ?? "—"}</TableCell>
                      <TableCell className="text-sm">{row.designationTitle ?? "—"}</TableCell>
                      <TableCell>{statusBadge(row.status)}</TableCell>
                      {isHrAdmin && (
                        <TableCell className="text-right">
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
                            <Button
                              size="sm"
                              variant={row.status === "HALF_DAY" ? "secondary" : "outline"}
                              className="gap-1 text-xs"
                              disabled={busy}
                              onClick={() => handleMark(row.employeeId, "HALF_DAY")}
                            >
                              <Clock className="h-3.5 w-3.5" />
                              Half Day
                            </Button>
                          </div>
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

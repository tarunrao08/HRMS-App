import React, { useEffect, useState, useCallback } from "react"
import { RefreshCw } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Combobox } from "@/components/ui/combobox"
import PageHeader from "@/components/shared/PageHeader"
import Pagination from "@/components/shared/Pagination"
import EmptyState from "@/components/shared/EmptyState"

import leaveService from "@/services/leaveService"
import { toastApiError } from "@/services/api"
import employeeService from "@/services/employeeService"
import type { LeaveRequest } from "@/services/leaveService"

type BadgeVariant = "default" | "secondary" | "destructive" | "outline" | "success" | "warning"

function statusVariant(status: string): BadgeVariant {
  switch (status) {
    case "APPROVED":  return "success"
    case "REJECTED":  return "destructive"
    case "PENDING":   return "warning"
    case "CANCELLED": return "secondary"
    default:          return "outline"
  }
}

interface EmployeeSummary { id: string; employeeCode: string; fullName: string }

const STATUSES = ["PENDING", "APPROVED", "REJECTED", "CANCELLED"]
const PAGE_SIZE = 20

// ── Main Page ─────────────────────────────────────────────────────────────────

export default function LeaveAdminPage() {
  const [employees, setEmployees]   = useState<EmployeeSummary[]>([])
  const [requests, setRequests]     = useState<LeaveRequest[]>([])
  const [loading, setLoading]       = useState(false)
  const [page, setPage]             = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)

  const [filterEmployee, setFilterEmployee] = useState("ALL")
  const [filterStatus, setFilterStatus]     = useState("ALL")

  useEffect(() => {
    employeeService.getSummaries()
      .then((res) => {
        const d = (res.data as any).data ?? res.data
        const list = Array.isArray(d) ? d : []
        setEmployees(list.map((e: any) => ({
          id: e.id,
          employeeCode: e.employeeCode,
          fullName: e.fullName,
        })))
      })
      .catch(() => {})
  }, [])

  const fetchAll = useCallback(async (p: number) => {
    setLoading(true)
    try {
      const res = await leaveService.getAllRequests({
        employeeId: filterEmployee === "ALL" ? undefined : filterEmployee,
        status:     filterStatus   === "ALL" ? undefined : filterStatus,
        page:       p,
        size:       PAGE_SIZE,
      })
      const d = (res.data as any).data ?? res.data
      setRequests(d.content ?? [])
      setTotalPages(d.totalPages ?? 0)
      setTotalElements(d.totalElements ?? 0)
    } catch (err) {
      toastApiError(err, "Failed to load leave requests")
    } finally {
      setLoading(false)
    }
  }, [filterEmployee, filterStatus])

  useEffect(() => { setPage(0); fetchAll(0) }, [fetchAll])

  return (
    <div className="space-y-6">
      <PageHeader
        title="Leave Admin"
        description="View and manage all employee leave requests."
        action={
          <Button variant="outline" size="icon" onClick={() => { setPage(0); fetchAll(0) }} disabled={loading} title="Refresh">
            <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
          </Button>
        }
      />

      {/* Filters */}
      <Card>
        <CardContent className="pt-4">
          <div className="flex flex-wrap items-center gap-3">
            <Combobox
              className="w-56"
              options={[
                { value: "ALL", label: "All Employees" },
                ...employees.map((e) => ({ value: e.id, label: `${e.fullName} (${e.employeeCode})` })),
              ]}
              value={filterEmployee}
              onChange={setFilterEmployee}
              placeholder="All Employees"
              searchPlaceholder="Search employees…"
            />

            <Select value={filterStatus} onValueChange={setFilterStatus}>
              <SelectTrigger className="w-40">
                <SelectValue placeholder="All Statuses" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All Statuses</SelectItem>
                {STATUSES.map((s) => (
                  <SelectItem key={s} value={s}>{s}</SelectItem>
                ))}
              </SelectContent>
            </Select>

            <Button variant="ghost" size="sm" onClick={() => { setFilterEmployee("ALL"); setFilterStatus("ALL") }}>
              Clear
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Table */}
      <Card>
        <CardContent className="p-0">
          {loading ? (
            <div className="p-6 space-y-3">
              {Array.from({ length: 6 }).map((_, i) => (
                <Skeleton key={i} className="h-10 w-full" />
              ))}
            </div>
          ) : requests.length === 0 ? (
            <EmptyState title="No leave requests" description="No requests match the selected filters." />
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Employee</TableHead>
                    <TableHead>Leave Type</TableHead>
                    <TableHead>From</TableHead>
                    <TableHead>To</TableHead>
                    <TableHead className="text-center">Days</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Applied</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {requests.map((r) => (
                    <TableRow key={r.id}>
                      <TableCell>
                        <div className="font-medium">{r.employeeName}</div>
                        <div className="text-xs text-muted-foreground">{r.employeeCode}</div>
                      </TableCell>
                      <TableCell>{r.leaveTypeName}</TableCell>
                      <TableCell className="whitespace-nowrap">{r.startDate}</TableCell>
                      <TableCell className="whitespace-nowrap">{r.endDate}</TableCell>
                      <TableCell className="text-center">{r.totalDays}</TableCell>
                      <TableCell>
                        <Badge variant={statusVariant(r.status)}>{r.status}</Badge>
                      </TableCell>
                      <TableCell className="whitespace-nowrap text-sm text-muted-foreground">
                        {new Date(r.appliedAt).toLocaleDateString()}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              <Pagination
                page={page}
                totalPages={totalPages}
                totalElements={totalElements}
                size={PAGE_SIZE}
                onPageChange={(p) => { setPage(p); fetchAll(p) }}
              />
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

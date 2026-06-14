import React, { useEffect, useState, useCallback } from "react"
import toast from "react-hot-toast"
import { RefreshCw, Loader2, X, Check } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Textarea } from "@/components/ui/textarea"
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

// ── Reject Dialog ─────────────────────────────────────────────────────────────

interface RejectDialogProps {
  open: boolean
  requestId: string
  onClose: () => void
  onSuccess: () => void
}

function RejectDialog({ open, requestId, onClose, onSuccess }: RejectDialogProps) {
  const [comments, setComments]     = useState("")
  const [submitting, setSubmitting] = useState(false)

  function handleClose() { setComments(""); onClose() }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!comments.trim()) { toast.error("Please provide a rejection reason"); return }
    setSubmitting(true)
    try {
      await leaveService.reject(requestId, comments.trim())
      toast.success("Leave request rejected")
      setComments("")
      onSuccess()
    } catch (err) {
      toastApiError(err, "Failed to reject")
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={(o) => !o && handleClose()}>
      <DialogContent className="sm:max-w-sm">
        <DialogHeader><DialogTitle>Reject Leave Request</DialogTitle></DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4 mt-2">
          <div className="space-y-1.5">
            <Label>Rejection Reason <span className="text-destructive">*</span></Label>
            <Textarea placeholder="State the reason for rejection…" value={comments}
              onChange={(e) => setComments(e.target.value)} rows={3} required />
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={handleClose} disabled={submitting}>Cancel</Button>
            <Button type="submit" variant="destructive" disabled={submitting}>
              {submitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Reject
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

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

  const [rejectId, setRejectId]     = useState("")
  const [rejectOpen, setRejectOpen] = useState(false)

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

  async function handleApprove(id: string) {
    try {
      await leaveService.approve(id)
      toast.success("Leave request approved")
      fetchAll(page)
    } catch (err) {
      toastApiError(err, "Failed to approve")
    }
  }

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
            <Select value={filterEmployee} onValueChange={setFilterEmployee}>
              <SelectTrigger className="w-56">
                <SelectValue placeholder="All Employees" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All Employees</SelectItem>
                {employees.map((e) => (
                  <SelectItem key={e.id} value={e.id}>
                    {e.fullName} ({e.employeeCode})
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

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
                    <TableHead className="text-right">Actions</TableHead>
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
                      <TableCell className="text-right">
                        {r.status === "PENDING" && (
                          <div className="flex items-center justify-end gap-1">
                            <Button size="icon" variant="ghost"
                              className="text-green-600 hover:text-green-700 hover:bg-green-50"
                              title="Approve" onClick={() => handleApprove(r.id)}>
                              <Check className="h-4 w-4" />
                            </Button>
                            <Button size="icon" variant="ghost"
                              className="text-destructive hover:text-destructive hover:bg-destructive/10"
                              title="Reject" onClick={() => { setRejectId(r.id); setRejectOpen(true) }}>
                              <X className="h-4 w-4" />
                            </Button>
                          </div>
                        )}
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

      <RejectDialog
        open={rejectOpen}
        requestId={rejectId}
        onClose={() => setRejectOpen(false)}
        onSuccess={() => { setRejectOpen(false); fetchAll(page) }}
      />
    </div>
  )
}

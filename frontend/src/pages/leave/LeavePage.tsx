import React, { useEffect, useState, useCallback } from "react"
import toast from "react-hot-toast"
import { Plus, Loader2 } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import PageHeader from "@/components/shared/PageHeader"
import Pagination from "@/components/shared/Pagination"
import EmptyState from "@/components/shared/EmptyState"

import { useAuthStore } from "@/store/authStore"
import leaveService from "@/services/leaveService"
import { toastApiError } from "@/services/api"
import type { LeaveType, LeaveBalance, LeaveRequest } from "@/services/leaveService"

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

const PAGE_SIZE = 10

// ── Apply Leave Dialog ────────────────────────────────────────────────────────

interface ApplyDialogProps {
  open: boolean
  onClose: () => void
  onSuccess: () => void
  leaveTypes: LeaveType[]
}

function ApplyLeaveDialog({ open, onClose, onSuccess, leaveTypes }: ApplyDialogProps) {
  const [leaveTypeId, setLeaveTypeId] = useState("")
  const [fromDate, setFromDate]       = useState("")
  const [toDate, setToDate]           = useState("")
  const [reason, setReason]           = useState("")
  const [submitting, setSubmitting]   = useState(false)

  function reset() {
    setLeaveTypeId(""); setFromDate(""); setToDate(""); setReason("")
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!leaveTypeId || !fromDate || !toDate || !reason.trim()) {
      toast.error("Please fill all required fields")
      return
    }
    if (toDate < fromDate) {
      toast.error("To date must be on or after From date")
      return
    }
    setSubmitting(true)
    try {
      await leaveService.apply({ leaveTypeId, startDate: fromDate, endDate: toDate, reason: reason.trim() })
      toast.success("Leave request submitted successfully")
      reset()
      onSuccess()
    } catch (err) {
      toastApiError(err, "Failed to submit leave request")
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={(o) => !o && (reset(), onClose())}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader><DialogTitle>Apply for Leave</DialogTitle></DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4 mt-2">
          <div className="space-y-1.5">
            <Label>Leave Type <span className="text-destructive">*</span></Label>
            <Select value={leaveTypeId} onValueChange={setLeaveTypeId}>
              <SelectTrigger><SelectValue placeholder="Select leave type" /></SelectTrigger>
              <SelectContent>
                {leaveTypes.map((lt) => (
                  <SelectItem key={lt.id} value={lt.id}>
                    {lt.name} ({lt.code}) — {lt.maxDaysPerYear}d/yr
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label>From Date <span className="text-destructive">*</span></Label>
              <Input type="date" value={fromDate} onChange={(e) => setFromDate(e.target.value)} required />
            </div>
            <div className="space-y-1.5">
              <Label>To Date <span className="text-destructive">*</span></Label>
              <Input type="date" value={toDate} min={fromDate || undefined} onChange={(e) => setToDate(e.target.value)} required />
            </div>
          </div>

          <div className="space-y-1.5">
            <Label>Reason <span className="text-destructive">*</span></Label>
            <Textarea placeholder="Briefly describe the reason for leave…" value={reason}
              onChange={(e) => setReason(e.target.value)} rows={3} required />
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => { reset(); onClose() }} disabled={submitting}>
              Cancel
            </Button>
            <Button type="submit" disabled={submitting}>
              {submitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Submit
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

// ── Main Page ─────────────────────────────────────────────────────────────────

export default function LeavePage() {
  const user = useAuthStore((s) => s.user)
  // user.id is set from the login response's employeeId field
  const hasEmployeeLink = Boolean(user?.id)

  const [leaveTypes, setLeaveTypes]   = useState<LeaveType[]>([])
  const [balances, setBalances]       = useState<LeaveBalance[]>([])
  const [requests, setRequests]       = useState<LeaveRequest[]>([])
  const [loadingBalances, setLoadingBalances] = useState(hasEmployeeLink)
  const [loadingRequests, setLoadingRequests] = useState(hasEmployeeLink)
  const [page, setPage]               = useState(0)
  const [totalPages, setTotalPages]   = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [applyOpen, setApplyOpen]     = useState(false)

  useEffect(() => {
    if (!hasEmployeeLink) return
    leaveService.getLeaveTypes()
      .then((res) => {
        const d = (res.data as any).data ?? res.data
        setLeaveTypes(Array.isArray(d) ? d : [])
      })
      .catch(() => {})

    fetchBalances()
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  function fetchBalances() {
    setLoadingBalances(true)
    leaveService.getMyBalances()
      .then((res) => {
        const d = (res.data as any).data ?? res.data
        setBalances(Array.isArray(d) ? d : [])
      })
      .catch(() => toastApiError(null, "Failed to load balances"))
      .finally(() => setLoadingBalances(false))
  }

  const fetchRequests = useCallback(async (p: number) => {
    if (!hasEmployeeLink) return
    setLoadingRequests(true)
    try {
      const res = await leaveService.getMyRequests({ page: p, size: PAGE_SIZE })
      const d = (res.data as any).data ?? res.data
      setRequests(d.content ?? [])
      setTotalPages(d.totalPages ?? 0)
      setTotalElements(d.totalElements ?? 0)
    } catch (err) {
      toastApiError(err, "Failed to load leave requests")
    } finally {
      setLoadingRequests(false)
    }
  }, [])

  useEffect(() => { fetchRequests(0) }, [fetchRequests])

  async function handleCancel(id: string) {
    try {
      await leaveService.cancel(id)
      toast.success("Leave request cancelled")
      fetchRequests(page)
      fetchBalances()
    } catch (err) {
      toastApiError(err, "Failed to cancel request")
    }
  }

  if (!hasEmployeeLink) {
    return (
      <div className="space-y-6">
        <PageHeader title="My Leave" description="View your leave balances and manage leave requests." />
        <EmptyState
          title="No employee record linked"
          description="Your user account is not linked to an employee record. Please ask your HR Admin to link your account to an employee profile before using the leave module."
        />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="My Leave"
        description="View your leave balances and manage leave requests."
        action={
          <Button onClick={() => setApplyOpen(true)}>
            <Plus className="mr-2 h-4 w-4" />
            Apply Leave
          </Button>
        }
      />

      {/* Balance Cards */}
      <div>
        <h2 className="mb-3 text-sm font-semibold text-muted-foreground uppercase tracking-wide">
          Leave Balances — {new Date().getFullYear()}
        </h2>
        {loadingBalances ? (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 4 }).map((_, i) => (
              <Skeleton key={i} className="h-32 rounded-lg" />
            ))}
          </div>
        ) : balances.length === 0 ? (
          <p className="text-sm text-muted-foreground">No leave balances configured for this year.</p>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {balances.map((b) => {
              const total   = b.allocatedDays + (b.availableDays < 0 ? 0 : 0)
              const used    = b.usedDays ?? 0
              const pending = b.pendingDays ?? 0
              const avail   = b.availableDays ?? 0
              return (
                <Card key={b.id}>
                  <CardHeader className="pb-2">
                    <CardTitle className="text-base">{b.leaveTypeName}</CardTitle>
                    <p className="text-xs text-muted-foreground">{b.leaveTypeCode} · {b.year}</p>
                  </CardHeader>
                  <CardContent>
                    <div className="flex justify-between text-sm">
                      <div className="text-center">
                        <div className="text-xl font-bold">{b.allocatedDays}</div>
                        <div className="text-xs text-muted-foreground">Allocated</div>
                      </div>
                      <div className="text-center">
                        <div className="text-xl font-bold text-amber-600">{used}</div>
                        <div className="text-xs text-muted-foreground">Used</div>
                      </div>
                      {pending > 0 && (
                        <div className="text-center">
                          <div className="text-xl font-bold text-orange-500">{pending}</div>
                          <div className="text-xs text-muted-foreground">Pending</div>
                        </div>
                      )}
                      <div className="text-center">
                        <div className="text-xl font-bold text-green-600">{avail}</div>
                        <div className="text-xs text-muted-foreground">Available</div>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              )
            })}
          </div>
        )}
      </div>

      {/* Request History */}
      <div>
        <h2 className="mb-3 text-sm font-semibold text-muted-foreground uppercase tracking-wide">
          My Leave Requests
        </h2>
        <Card>
          <CardContent className="p-0">
            {loadingRequests ? (
              <div className="p-6 space-y-3">
                {Array.from({ length: 5 }).map((_, i) => (
                  <Skeleton key={i} className="h-10 w-full" />
                ))}
              </div>
            ) : requests.length === 0 ? (
              <EmptyState title="No leave requests" description="You haven't applied for any leave yet." />
            ) : (
              <>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Leave Type</TableHead>
                      <TableHead>From</TableHead>
                      <TableHead>To</TableHead>
                      <TableHead className="text-center">Days</TableHead>
                      <TableHead>Reason</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Applied</TableHead>
                      <TableHead className="text-right">Action</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {requests.map((r) => (
                      <TableRow key={r.id}>
                        <TableCell className="font-medium">{r.leaveTypeName}</TableCell>
                        <TableCell className="whitespace-nowrap">{r.startDate}</TableCell>
                        <TableCell className="whitespace-nowrap">{r.endDate}</TableCell>
                        <TableCell className="text-center">{r.totalDays}</TableCell>
                        <TableCell className="max-w-[160px] truncate" title={r.reason}>{r.reason}</TableCell>
                        <TableCell>
                          <Badge variant={statusVariant(r.status)}>{r.status}</Badge>
                        </TableCell>
                        <TableCell className="whitespace-nowrap text-sm text-muted-foreground">
                          {new Date(r.appliedAt).toLocaleDateString()}
                        </TableCell>
                        <TableCell className="text-right">
                          {r.status === "PENDING" && (
                            <Button size="sm" variant="outline"
                              className="text-destructive border-destructive/30 hover:bg-destructive/10"
                              onClick={() => handleCancel(r.id)}>
                              Cancel
                            </Button>
                          )}
                          {r.rejectionReason && (
                            <span className="text-xs text-muted-foreground" title={r.rejectionReason}>
                              Rejected
                            </span>
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
                  onPageChange={(p) => { setPage(p); fetchRequests(p) }}
                />
              </>
            )}
          </CardContent>
        </Card>
      </div>

      <ApplyLeaveDialog
        open={applyOpen}
        onClose={() => setApplyOpen(false)}
        onSuccess={() => { setApplyOpen(false); fetchRequests(0); fetchBalances() }}
        leaveTypes={leaveTypes}
      />
    </div>
  )
}

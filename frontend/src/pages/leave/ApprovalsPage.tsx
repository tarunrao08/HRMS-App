import React, { useEffect, useState, useCallback } from "react"
import toast from "react-hot-toast"
import { Check, X, Loader2, RefreshCw } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Textarea } from "@/components/ui/textarea"
import PageHeader from "@/components/shared/PageHeader"
import EmptyState from "@/components/shared/EmptyState"

import { useAuthStore } from "@/store/authStore"
import leaveService from "@/services/leaveService"
import { toastApiError } from "@/services/api"
import type { LeaveRequest } from "@/services/leaveService"

// ── Reject Dialog ─────────────────────────────────────────────────────────────

interface RejectDialogProps {
  open: boolean
  requestId: string
  onClose: () => void
  onSuccess: () => void
}

function RejectDialog({ open, requestId, onClose, onSuccess }: RejectDialogProps) {
  const [comments, setComments]       = useState("")
  const [submitting, setSubmitting]   = useState(false)

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
      toastApiError(err, "Failed to reject leave request")
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

export default function ApprovalsPage() {
  const user = useAuthStore((s) => s.user)
  const hasEmployeeLink = Boolean(user?.id)

  const [requests, setRequests]   = useState<LeaveRequest[]>([])
  const [loading, setLoading]     = useState(false)
  const [rejectId, setRejectId]   = useState("")
  const [rejectOpen, setRejectOpen] = useState(false)

  const fetchPending = useCallback(async () => {
    if (!hasEmployeeLink) return
    setLoading(true)
    try {
      const res = await leaveService.getPendingApprovals()
      const d = (res.data as any).data ?? res.data
      setRequests(Array.isArray(d) ? d : [])
    } catch (err) {
      toastApiError(err, "Failed to load pending approvals")
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { fetchPending() }, [fetchPending])

  async function handleApprove(id: string) {
    try {
      await leaveService.approve(id)
      toast.success("Leave request approved")
      fetchPending()
    } catch (err) {
      toastApiError(err, "Failed to approve")
    }
  }

  if (!hasEmployeeLink) {
    return (
      <div className="space-y-6">
        <PageHeader title="Pending Approvals" description="Leave requests waiting for your approval." />
        <EmptyState
          title="No employee record linked"
          description="Your user account is not linked to an employee record. You cannot act as an approver until your account is linked to an employee profile."
        />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Pending Approvals"
        description="Leave requests waiting for your approval."
        action={
          <Button variant="outline" size="icon" onClick={fetchPending} disabled={loading} title="Refresh">
            <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
          </Button>
        }
      />

      <Card>
        <CardContent className="p-0">
          {loading ? (
            <div className="p-6 space-y-3">
              {Array.from({ length: 4 }).map((_, i) => (
                <Skeleton key={i} className="h-10 w-full" />
              ))}
            </div>
          ) : requests.length === 0 ? (
            <EmptyState
              title="No pending approvals"
              description="All leave requests in your queue have been handled."
            />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Employee</TableHead>
                  <TableHead>Leave Type</TableHead>
                  <TableHead>From</TableHead>
                  <TableHead>To</TableHead>
                  <TableHead className="text-center">Days</TableHead>
                  <TableHead>Reason</TableHead>
                  <TableHead>Level</TableHead>
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
                    <TableCell className="max-w-[160px] truncate" title={r.reason}>{r.reason}</TableCell>
                    <TableCell>
                      <Badge variant="outline">L{r.currentApprovalLevel}</Badge>
                    </TableCell>
                    <TableCell className="whitespace-nowrap text-sm text-muted-foreground">
                      {new Date(r.appliedAt).toLocaleDateString()}
                    </TableCell>
                    <TableCell className="text-right">
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
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <RejectDialog
        open={rejectOpen}
        requestId={rejectId}
        onClose={() => setRejectOpen(false)}
        onSuccess={() => { setRejectOpen(false); fetchPending() }}
      />
    </div>
  )
}

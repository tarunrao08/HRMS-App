import React, { useEffect, useState, useCallback } from "react"
import toast from "react-hot-toast"
import { History, Loader2 } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import { Card, CardContent } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Combobox } from "@/components/ui/combobox"
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import PageHeader from "@/components/shared/PageHeader"
import EmptyState from "@/components/shared/EmptyState"
import ConfirmDialog from "@/components/shared/ConfirmDialog"

import { useAuthStore } from "@/store/authStore"
import { useIsHrAdmin } from "@/hooks/useRole"
import employeeService from "@/services/employeeService"
import type { EmployeeSummary, EmployeeNameResponse } from "@/services/employeeService"
import leaveService from "@/services/leaveService"
import type { LeaveBalance, LeaveBalanceAdjustment } from "@/services/leaveService"
import { toastApiError } from "@/services/api"
import { useFormSnapshot } from "@/hooks/useFormSnapshot"
import { useConfirmClose } from "@/hooks/useConfirmClose"

const YEAR = new Date().getFullYear()

// ── Adjust Dialog ─────────────────────────────────────────────────────────────

interface AdjustDialogProps {
  open: boolean
  employeeId: string
  balance: LeaveBalance | null
  onClose: () => void
  onSuccess: () => void
}

function AdjustDialog({ open, employeeId, balance, onClose, onSuccess }: AdjustDialogProps) {
  const [days, setDays]           = useState("")
  const [reason, setReason]       = useState("")
  const [submitting, setSubmitting] = useState(false)

  function reset() { setDays(""); setReason("") }
  function handleClose() { reset(); onClose() }

  const { isDirty } = useFormSnapshot(open, { days, reason })
  const { confirmOpen, setConfirmOpen, requestClose, confirmDiscard } = useConfirmClose(handleClose)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const delta = Number(days)
    if (!days.trim() || Number.isNaN(delta) || delta === 0) {
      toast.error("Enter a non-zero number of days (positive to add, negative to deduct)")
      return
    }
    if (!reason.trim()) {
      toast.error("A reason is required")
      return
    }
    if (!balance) return
    setSubmitting(true)
    try {
      await leaveService.adjustBalance(employeeId, {
        leaveTypeId: balance.leaveTypeId,
        year: YEAR,
        days: delta,
        reason: reason.trim(),
      })
      toast.success("Leave balance adjusted successfully")
      reset()
      onSuccess()
    } catch (err) {
      toastApiError(err, "Failed to adjust leave balance")
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <>
    <Dialog open={open} onOpenChange={(o) => { if (!o) requestClose(isDirty()) }}>
      <DialogContent className="sm:max-w-sm">
        <DialogHeader>
          <DialogTitle>Adjust {balance?.leaveTypeName ?? "Leave"} Balance</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4 mt-2">
          <p className="text-sm text-muted-foreground">
            Currently allocated: <span className="font-medium text-foreground">{balance?.allocatedDays}</span> days
          </p>
          <div className="space-y-1.5">
            <Label>Days (+ to add, − to deduct) <span className="text-destructive">*</span></Label>
            <Input
              type="number"
              step="0.5"
              placeholder="e.g. 2 or -1.5"
              value={days}
              onChange={(e) => setDays(e.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <Label>Reason <span className="text-destructive">*</span></Label>
            <Textarea
              placeholder="e.g. Comp-off for weekend on-call work"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              rows={3}
            />
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => requestClose(isDirty())} disabled={submitting}>Cancel</Button>
            <Button type="submit" disabled={submitting}>
              {submitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Adjust
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
    <ConfirmDialog
      open={confirmOpen}
      onOpenChange={setConfirmOpen}
      title="Discard changes?"
      description="You have unsaved changes in this form. Are you sure you want to cancel? Your changes will be lost."
      confirmLabel="Discard"
      cancelLabel="Keep Editing"
      onConfirm={confirmDiscard}
    />
    </>
  )
}

// ── History Dialog ────────────────────────────────────────────────────────────

interface HistoryDialogProps {
  open: boolean
  employeeId: string
  balance: LeaveBalance | null
  onClose: () => void
}

function HistoryDialog({ open, employeeId, balance, onClose }: HistoryDialogProps) {
  const [history, setHistory] = useState<LeaveBalanceAdjustment[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!open || !balance) return
    setLoading(true)
    leaveService.getAdjustmentHistory(employeeId, balance.leaveTypeId, YEAR)
      .then((res) => {
        const d = (res.data as any).data ?? res.data
        setHistory(Array.isArray(d) ? d : [])
      })
      .catch(() => toastApiError(null, "Failed to load adjustment history"))
      .finally(() => setLoading(false))
  }, [open, balance, employeeId])

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{balance?.leaveTypeName ?? "Leave"} Adjustment History</DialogTitle>
        </DialogHeader>
        {loading ? (
          <div className="space-y-2">
            {Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-10 w-full" />)}
          </div>
        ) : history.length === 0 ? (
          <p className="text-sm text-muted-foreground py-4">No adjustments recorded yet.</p>
        ) : (
          <div className="space-y-3 max-h-80 overflow-y-auto">
            {history.map((h) => (
              <div key={h.id} className="rounded-md border p-3 text-sm">
                <div className="flex justify-between">
                  <span className={`font-semibold ${h.days > 0 ? "text-green-600" : "text-destructive"}`}>
                    {h.days > 0 ? "+" : ""}{h.days} days
                  </span>
                  <span className="text-xs text-muted-foreground">
                    {new Date(h.createdAt).toLocaleDateString()}
                  </span>
                </div>
                <p className="text-muted-foreground mt-1">{h.reason}</p>
                <p className="text-xs text-muted-foreground mt-1">by {h.adjustedByName}</p>
              </div>
            ))}
          </div>
        )}
        <DialogFooter>
          <Button type="button" variant="outline" onClick={onClose}>Close</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

// ── Main Page ─────────────────────────────────────────────────────────────────

export default function LeaveBalancesPage() {
  const user = useAuthStore((s) => s.user)
  const isHrAdmin = useIsHrAdmin()

  const [employees, setEmployees]   = useState<(EmployeeSummary | EmployeeNameResponse)[]>([])
  const [employeeId, setEmployeeId] = useState("")
  const [balances, setBalances]     = useState<LeaveBalance[]>([])
  const [loadingEmployees, setLoadingEmployees] = useState(true)
  const [loadingBalances, setLoadingBalances]   = useState(false)

  const [adjustTarget, setAdjustTarget] = useState<LeaveBalance | null>(null)
  const [historyTarget, setHistoryTarget] = useState<LeaveBalance | null>(null)

  useEffect(() => {
    setLoadingEmployees(true)
    const request = isHrAdmin
      ? employeeService.getSummaries()
      : employeeService.getDirectReports(user?.id ?? "")

    request
      .then((res) => {
        const d = (res.data as any).data ?? res.data
        setEmployees(Array.isArray(d) ? d : [])
      })
      .catch(() => toastApiError(null, "Failed to load employees"))
      .finally(() => setLoadingEmployees(false))
  }, [isHrAdmin, user?.id])

  const fetchBalances = useCallback(() => {
    if (!employeeId) return
    setLoadingBalances(true)
    leaveService.getBalancesForEmployee(employeeId, YEAR)
      .then((res) => {
        const d = (res.data as any).data ?? res.data
        setBalances(Array.isArray(d) ? d : [])
      })
      .catch((err) => toastApiError(err, "Failed to load leave balances"))
      .finally(() => setLoadingBalances(false))
  }, [employeeId])

  useEffect(() => { fetchBalances() }, [fetchBalances])

  function employeeLabel(e: EmployeeSummary | EmployeeNameResponse): string {
    if ("fullName" in e) return `${e.fullName} (${e.employeeCode})`
    return `${e.firstName} ${e.lastName} (${e.employeeCode})`
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Leave Balances"
        description={isHrAdmin
          ? "View and adjust any employee's leave balance."
          : "View and adjust your direct reports' leave balances."}
      />

      <Card>
        <CardContent className="pt-4">
          <div className="max-w-sm space-y-1.5">
            <Label>Employee</Label>
            {loadingEmployees ? (
              <Skeleton className="h-10 w-full" />
            ) : (
              <Combobox
                options={employees.map((e) => ({ value: e.id, label: employeeLabel(e) }))}
                value={employeeId}
                onChange={setEmployeeId}
                placeholder="Select an employee"
                searchPlaceholder="Search employees…"
              />
            )}
          </div>
        </CardContent>
      </Card>

      {!employeeId ? (
        <EmptyState title="Select an employee" description="Choose an employee above to view their leave balances." />
      ) : (
        <Card>
          <CardContent className="p-0">
            {loadingBalances ? (
              <div className="p-6 space-y-3">
                {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-10 w-full" />)}
              </div>
            ) : balances.length === 0 ? (
              <EmptyState title="No balances found" description={`No leave balances configured for ${YEAR}.`} />
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Leave Type</TableHead>
                    <TableHead className="text-center">Allocated</TableHead>
                    <TableHead className="text-center">Used</TableHead>
                    <TableHead className="text-center">Pending</TableHead>
                    <TableHead className="text-center">Available</TableHead>
                    <TableHead className="text-right">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {balances.map((b) => (
                    <TableRow key={b.id}>
                      <TableCell className="font-medium">{b.leaveTypeName}</TableCell>
                      <TableCell className="text-center">{b.allocatedDays}</TableCell>
                      <TableCell className="text-center text-amber-600">{b.usedDays}</TableCell>
                      <TableCell className="text-center text-orange-500">{b.pendingDays}</TableCell>
                      <TableCell className="text-center text-green-600 font-semibold">{b.availableDays}</TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-1">
                          <Button size="sm" variant="outline" onClick={() => setAdjustTarget(b)}>
                            Adjust
                          </Button>
                          <Button size="icon" variant="ghost" title="History" onClick={() => setHistoryTarget(b)}>
                            <History className="h-4 w-4" />
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
      )}

      <AdjustDialog
        open={!!adjustTarget}
        employeeId={employeeId}
        balance={adjustTarget}
        onClose={() => setAdjustTarget(null)}
        onSuccess={() => { setAdjustTarget(null); fetchBalances() }}
      />

      <HistoryDialog
        open={!!historyTarget}
        employeeId={employeeId}
        balance={historyTarget}
        onClose={() => setHistoryTarget(null)}
      />
    </div>
  )
}

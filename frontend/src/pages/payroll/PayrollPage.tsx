import React, { useEffect, useState, useCallback } from "react"
import toast from "react-hot-toast"
import { ArrowLeft, Plus, FileDown, Wallet, FileText } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Select, SelectTrigger, SelectValue, SelectContent, SelectItem,
} from "@/components/ui/select"
import {
  Table, TableHeader, TableBody, TableRow, TableHead, TableCell,
} from "@/components/ui/table"
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogClose,
} from "@/components/ui/dialog"
import PageHeader from "@/components/shared/PageHeader"
import EmptyState from "@/components/shared/EmptyState"
import payrollService, {
  type PayrollRun, type Payslip, type SalaryStructureResponse,
} from "@/services/payrollService"
import employeeService from "@/services/employeeService"
import { toastApiError } from "@/services/api"
import { useIsHrAdmin, useIsManagerOrAbove } from "@/hooks/useRole"

// ── Constants ─────────────────────────────────────────────────────────────────

const MONTH_NAMES = [
  "January","February","March","April","May","June",
  "July","August","September","October","November","December",
]

function inr(amount: number | undefined | null): string {
  if (amount == null) return "₹0"
  return "₹" + Number(amount).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function fmtDate(iso?: string): string {
  if (!iso) return "—"
  return new Date(iso).toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" })
}

function runBadge(status: string): "secondary" | "warning" | "success" | "default" {
  if (status === "DRAFT") return "secondary"
  if (status === "PROCESSED") return "warning"
  if (status === "APPROVED") return "success"
  return "default"
}

// ── CTC breakdown preview (pure computation, same as backend) ─────────────────

function computeBreakdown(annualCtc: number) {
  const monthly = annualCtc / 12
  const basic       = monthly * 0.40
  const hra         = monthly * 0.20
  const da          = monthly * 0.10
  const conveyance  = monthly * 0.10
  const medical     = monthly * 0.05
  const special     = monthly - basic - hra - da - conveyance - medical
  return { monthly, basic, hra, da, conveyance, medical, special }
}

// ── Payslip detail dialog ─────────────────────────────────────────────────────

function PayslipDetailDialog({ payslip, onClose }: { payslip: Payslip; onClose: () => void }) {
  const title = `${payslip.employeeName} — ${MONTH_NAMES[payslip.month - 1]} ${payslip.year}`
  return (
    <Dialog open onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader><DialogTitle>{title}</DialogTitle></DialogHeader>

        {/* Days row */}
        <div className="grid grid-cols-3 gap-3 rounded-lg bg-muted/40 p-3 text-center text-sm">
          <div>
            <p className="text-xs text-muted-foreground">Working Days</p>
            <p className="font-semibold">{payslip.workingDays}</p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground">LOP Days</p>
            <p className="font-semibold">{payslip.lopDays}</p>
          </div>
          <div>
            <p className="text-xs text-muted-foreground">Paid Days</p>
            <p className="font-semibold">{payslip.paidDays}</p>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4 mt-1">
          {/* Earnings */}
          <div>
            <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">Earnings</p>
            <table className="w-full text-sm">
              <tbody>
                {[
                  ["Basic",            payslip.basic],
                  ["HRA",              payslip.hra],
                  ["Dearness Allow.",  payslip.da],
                  ["Conveyance",       payslip.conveyance],
                  ["Medical Allow.",   payslip.medicalAllowance],
                  ["Special Allow.",   payslip.specialAllowance],
                ].map(([label, val]) => (
                  <tr key={label as string} className="border-b last:border-0">
                    <td className="py-1 text-muted-foreground">{label}</td>
                    <td className="py-1 text-right font-mono">{inr(val as number)}</td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr className="font-semibold">
                  <td className="pt-2">Gross Pay</td>
                  <td className="pt-2 text-right font-mono">{inr(payslip.grossSalary)}</td>
                </tr>
              </tfoot>
            </table>
          </div>

          {/* Deductions */}
          <div>
            <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">Deductions</p>
            <table className="w-full text-sm">
              <tbody>
                <tr className="border-b">
                  <td className="py-1 text-muted-foreground">Tax (TDS)</td>
                  <td className="py-1 text-right font-mono">{inr(payslip.tds)}</td>
                </tr>
              </tbody>
              <tfoot>
                <tr className="font-semibold">
                  <td className="pt-2">Total Ded.</td>
                  <td className="pt-2 text-right font-mono">{inr(payslip.totalDeductions)}</td>
                </tr>
              </tfoot>
            </table>
          </div>
        </div>

        {/* Net pay banner */}
        <div className="flex items-center justify-between rounded-lg bg-primary px-4 py-3 text-primary-foreground">
          <span className="font-semibold">Net Pay</span>
          <span className="text-xl font-bold font-mono">{inr(payslip.netSalary)}</span>
        </div>

        <DialogFooter>
          {payslip.pdfUrl && (
            <a
              href={`/api/payroll/payslips/${payslip.id}/pdf`}
              target="_blank"
              rel="noreferrer"
            >
              <Button variant="outline" size="sm">
                <FileDown className="mr-2 h-4 w-4" />
                Download PDF
              </Button>
            </a>
          )}
          <DialogClose asChild><Button>Close</Button></DialogClose>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

// ── Payslips sub-view ─────────────────────────────────────────────────────────

interface PayslipsViewProps { run: PayrollRun; onBack: () => void }

function PayslipsView({ run, onBack }: PayslipsViewProps) {
  const [payslips, setPayslips] = useState<Payslip[]>([])
  const [loading, setLoading] = useState(true)
  const [detail, setDetail] = useState<Payslip | null>(null)

  useEffect(() => {
    setLoading(true)
    payrollService.getPayslips(run.id)
      .then((res) => {
        const d = (res.data as any)?.data ?? res.data
        setPayslips(Array.isArray(d) ? d : (d?.content ?? []))
      })
      .catch((err) => toastApiError(err, "Failed to load payslips"))
      .finally(() => setLoading(false))
  }, [run.id])

  return (
    <div className="space-y-4">
      <Button variant="ghost" size="sm" onClick={onBack} className="gap-1">
        <ArrowLeft className="h-4 w-4" /> Back to Payroll Runs
      </Button>

      <PageHeader
        title={`Payslips — ${MONTH_NAMES[run.month - 1]} ${run.year}`}
        description={`${run.totalEmployees} employee(s) · ${inr(run.totalNet)} net pay`}
      />

      <Card>
        <CardContent className="p-0">
          {loading ? (
            <div className="space-y-3 p-6">
              {Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-10 w-full" />)}
            </div>
          ) : payslips.length === 0 ? (
            <EmptyState title="No payslips found" description="Process the payroll run to generate payslips." />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Code</TableHead>
                  <TableHead>Employee</TableHead>
                  <TableHead className="text-right">Gross</TableHead>
                  <TableHead className="text-right">TDS</TableHead>
                  <TableHead className="text-right">Net Pay</TableHead>
                  <TableHead className="text-center">Days</TableHead>
                  <TableHead></TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {payslips.map((p) => (
                  <TableRow key={p.id} className="cursor-pointer" onClick={() => setDetail(p)}>
                    <TableCell className="font-mono text-sm">{p.employeeCode}</TableCell>
                    <TableCell>{p.employeeName}</TableCell>
                    <TableCell className="text-right">{inr(p.grossSalary)}</TableCell>
                    <TableCell className="text-right text-muted-foreground">{inr(p.tds)}</TableCell>
                    <TableCell className="text-right font-semibold">{inr(p.netSalary)}</TableCell>
                    <TableCell className="text-center text-sm text-muted-foreground">
                      {p.paidDays}/{p.workingDays}
                    </TableCell>
                    <TableCell>
                      <Button size="sm" variant="ghost" onClick={(e) => { e.stopPropagation(); setDetail(p) }}>
                        Details
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {detail && <PayslipDetailDialog payslip={detail} onClose={() => setDetail(null)} />}
    </div>
  )
}

// ── New Payroll Run dialog ────────────────────────────────────────────────────

interface NewRunDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onCreated: (run: PayrollRun) => void
}

function NewRunDialog({ open, onOpenChange, onCreated }: NewRunDialogProps) {
  const currentYear = new Date().getFullYear()
  const [month, setMonth] = useState<string>(String(new Date().getMonth() + 1))
  const [year, setYear]   = useState<string>(String(currentYear))
  const [saving, setSaving] = useState(false)

  function handleSubmit() {
    const m = parseInt(month, 10)
    const y = parseInt(year, 10)
    if (!m || !y || y < 2000 || y > 2100) {
      toast.error("Please enter a valid month and year.")
      return
    }
    setSaving(true)
    payrollService.createRun(m, y)
      .then((res) => {
        const run: PayrollRun = (res.data as any)?.data ?? res.data
        toast.success("Payroll run created.")
        onCreated(run)
        onOpenChange(false)
      })
      .catch((err) => toastApiError(err, "Failed to create payroll run."))
      .finally(() => setSaving(false))
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-sm">
        <DialogHeader><DialogTitle>New Payroll Run</DialogTitle></DialogHeader>
        <div className="space-y-4 py-2">
          <div className="space-y-1.5">
            <Label>Month</Label>
            <Select value={month} onValueChange={setMonth}>
              <SelectTrigger><SelectValue placeholder="Select month" /></SelectTrigger>
              <SelectContent>
                {MONTH_NAMES.map((name, idx) => (
                  <SelectItem key={idx + 1} value={String(idx + 1)}>{name}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-1.5">
            <Label>Year</Label>
            <Input type="number" min={2000} max={2100} value={year}
              onChange={(e) => setYear(e.target.value)} placeholder="e.g. 2025" />
          </div>
        </div>
        <DialogFooter>
          <DialogClose asChild><Button variant="outline" disabled={saving}>Cancel</Button></DialogClose>
          <Button onClick={handleSubmit} disabled={saving}>{saving ? "Creating…" : "Create Run"}</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

// ── Salary Structure dialog ───────────────────────────────────────────────────

interface SalaryStructureDialogProps {
  open: boolean
  onOpenChange: (v: boolean) => void
}

function SalaryStructureDialog({ open, onOpenChange }: SalaryStructureDialogProps) {
  const [employees, setEmployees] = useState<{ id: string; fullName: string; employeeCode: string }[]>([])
  const [employeeId, setEmployeeId]   = useState("NONE")
  const [annualCtc, setAnnualCtc]     = useState("")
  const [effectiveFrom, setEffectiveFrom] = useState(
    new Date().toISOString().slice(0, 10)
  )
  const [saving, setSaving]           = useState(false)
  const [existing, setExisting]       = useState<SalaryStructureResponse | null>(null)

  useEffect(() => {
    if (!open) return
    employeeService.getSummaries()
      .then((res) => {
        const d = (res.data as any)?.data ?? res.data
        setEmployees(Array.isArray(d) ? d : [])
      })
      .catch(() => {})
  }, [open])

  // Load current active structure when employee changes
  useEffect(() => {
    if (employeeId === "NONE") { setExisting(null); return }
    payrollService.getActiveSalaryStructure(employeeId)
      .then((res) => {
        const d = (res.data as any)?.data ?? res.data
        setExisting(d ?? null)
        if (d?.annualCtc) setAnnualCtc(String(d.annualCtc))
      })
      .catch(() => setExisting(null))
  }, [employeeId])

  const ctcNum = parseFloat(annualCtc) || 0
  const preview = ctcNum > 0 ? computeBreakdown(ctcNum) : null

  function handleSave() {
    if (employeeId === "NONE" || !ctcNum || !effectiveFrom) {
      toast.error("Please fill all fields.")
      return
    }
    setSaving(true)
    payrollService.createSalaryStructure({ employeeId, annualCtc: ctcNum, effectiveFrom })
      .then(() => {
        toast.success("Salary structure saved.")
        onOpenChange(false)
        setAnnualCtc(""); setEmployeeId("NONE"); setExisting(null)
      })
      .catch((err) => toastApiError(err, "Failed to save salary structure."))
      .finally(() => setSaving(false))
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader><DialogTitle>Set Employee CTC</DialogTitle></DialogHeader>

        <div className="space-y-4">
          {/* Employee picker */}
          <div className="space-y-1.5">
            <Label>Employee</Label>
            <Select value={employeeId} onValueChange={setEmployeeId}>
              <SelectTrigger><SelectValue placeholder="Select employee" /></SelectTrigger>
              <SelectContent>
                <SelectItem value="NONE">— Select —</SelectItem>
                {employees.map((e) => (
                  <SelectItem key={e.id} value={e.id}>
                    {e.fullName} ({e.employeeCode})
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {existing && (
              <p className="text-xs text-muted-foreground">
                Current CTC: {inr(existing.annualCtc)}/yr (effective {existing.effectiveFrom})
              </p>
            )}
          </div>

          {/* Annual CTC */}
          <div className="space-y-1.5">
            <Label>Annual CTC (₹)</Label>
            <Input type="number" min={0} placeholder="e.g. 600000"
              value={annualCtc} onChange={(e) => setAnnualCtc(e.target.value)} />
          </div>

          {/* Effective From */}
          <div className="space-y-1.5">
            <Label>Effective From</Label>
            <Input type="date" value={effectiveFrom}
              onChange={(e) => setEffectiveFrom(e.target.value)} />
          </div>

          {/* Live breakdown preview */}
          {preview && (
            <div className="rounded-lg border bg-muted/30 p-3">
              <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                Monthly Breakdown Preview
              </p>
              <table className="w-full text-sm">
                <tbody>
                  {[
                    ["Monthly Gross",       preview.monthly],
                    ["Basic (40%)",         preview.basic],
                    ["HRA (20%)",           preview.hra],
                    ["DA (10%)",            preview.da],
                    ["Conveyance (10%)",    preview.conveyance],
                    ["Medical Allow. (5%)", preview.medical],
                    ["Special Allow.",      preview.special],
                  ].map(([label, val]) => (
                    <tr key={label as string} className="border-b last:border-0">
                      <td className="py-0.5 text-muted-foreground">{label}</td>
                      <td className="py-0.5 text-right font-mono text-xs">{inr(val as number)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        <DialogFooter>
          <DialogClose asChild><Button variant="outline" disabled={saving}>Cancel</Button></DialogClose>
          <Button onClick={handleSave} disabled={saving || employeeId === "NONE"}>
            {saving ? "Saving…" : "Save CTC"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

// ── Generate Payslip dialog ───────────────────────────────────────────────────

interface GeneratePayslipDialogProps {
  open: boolean
  onOpenChange: (v: boolean) => void
}

function GeneratePayslipDialog({ open, onOpenChange }: GeneratePayslipDialogProps) {
  const currentYear  = new Date().getFullYear()
  const currentMonth = new Date().getMonth() + 1

  const [employees, setEmployees]     = useState<{ id: string; fullName: string; employeeCode: string }[]>([])
  const [employeeId, setEmployeeId]   = useState("NONE")
  const [month, setMonth]             = useState<string>(String(currentMonth))
  const [year, setYear]               = useState<string>(String(currentYear))
  const [saving, setSaving]           = useState(false)
  const [generated, setGenerated]     = useState<Payslip | null>(null)

  useEffect(() => {
    if (!open) return
    employeeService.getSummaries()
      .then((res) => {
        const d = (res.data as any)?.data ?? res.data
        setEmployees(Array.isArray(d) ? d : [])
      })
      .catch(() => {})
  }, [open])

  function handleClose() {
    setEmployeeId("NONE")
    setMonth(String(currentMonth))
    setYear(String(currentYear))
    setGenerated(null)
    onOpenChange(false)
  }

  function handleGenerate() {
    const m = parseInt(month, 10)
    const y = parseInt(year, 10)
    if (employeeId === "NONE" || !m || !y) {
      toast.error("Please select an employee, month, and year.")
      return
    }
    setSaving(true)
    payrollService.generatePayslip(employeeId, m, y)
      .then((res) => {
        const payslip: Payslip = (res.data as any)?.data ?? res.data
        setGenerated(payslip)
        toast.success(`Payslip generated for ${payslip.employeeName} — ${MONTH_NAMES[payslip.month - 1]} ${payslip.year}`)
      })
      .catch((err) => toastApiError(err, "Failed to generate payslip."))
      .finally(() => setSaving(false))
  }

  return (
    <Dialog open={open} onOpenChange={(o) => { if (!o) handleClose() }}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader><DialogTitle>Generate Payslip</DialogTitle></DialogHeader>

        {generated ? (
          <div className="space-y-4">
            <div className="rounded-lg border bg-green-50 p-4 text-sm space-y-2">
              <p className="font-semibold text-green-800">
                Payslip generated &amp; published for {generated.employeeName}
              </p>
              <div className="flex justify-between text-muted-foreground">
                <span>Period</span>
                <span>{MONTH_NAMES[generated.month - 1]} {generated.year}</span>
              </div>
              <div className="flex justify-between text-muted-foreground">
                <span>Gross Pay</span>
                <span>{inr(generated.grossSalary)}</span>
              </div>
              <div className="flex justify-between text-muted-foreground">
                <span>Total Deductions</span>
                <span>{inr(generated.totalDeductions)}</span>
              </div>
              <div className="flex justify-between font-semibold border-t pt-2">
                <span>Net Pay</span>
                <span>{inr(generated.netSalary)}</span>
              </div>
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => setGenerated(null)}>Generate Another</Button>
              <Button onClick={handleClose}>Done</Button>
            </DialogFooter>
          </div>
        ) : (
          <div className="space-y-4">
            <div className="space-y-1.5">
              <Label>Employee</Label>
              <Select value={employeeId} onValueChange={setEmployeeId}>
                <SelectTrigger><SelectValue placeholder="Select employee" /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="NONE">— Select —</SelectItem>
                  {employees.map((e) => (
                    <SelectItem key={e.id} value={e.id}>
                      {e.fullName} ({e.employeeCode})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label>Month</Label>
                <Select value={month} onValueChange={setMonth}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {MONTH_NAMES.map((name, idx) => (
                      <SelectItem key={idx + 1} value={String(idx + 1)}>{name}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-1.5">
                <Label>Year</Label>
                <Input type="number" min={2000} max={2100} value={year}
                  onChange={(e) => setYear(e.target.value)} />
              </div>
            </div>

            <DialogFooter>
              <DialogClose asChild><Button variant="outline" disabled={saving}>Cancel</Button></DialogClose>
              <Button onClick={handleGenerate} disabled={saving || employeeId === "NONE"}>
                {saving ? "Generating…" : "Generate"}
              </Button>
            </DialogFooter>
          </div>
        )}
      </DialogContent>
    </Dialog>
  )
}

// ── Main page ─────────────────────────────────────────────────────────────────

export default function PayrollPage() {
  const isHrAdmin = useIsHrAdmin()
  const isPrivileged = useIsManagerOrAbove()

  const [runs, setRuns]           = useState<PayrollRun[]>([])
  const [loading, setLoading]     = useState(true)
  const [activeRun, setActiveRun] = useState<PayrollRun | null>(null)
  const [showNewRun, setShowNewRun]         = useState(false)
  const [showSetCtc, setShowSetCtc]         = useState(false)
  const [showGenerate, setShowGenerate]     = useState(false)
  const [actionLoading, setActionLoading]   = useState<string | null>(null)

  const loadRuns = useCallback(() => {
    setLoading(true)
    payrollService.getRuns()
      .then((res) => {
        const d = (res.data as any)?.data ?? res.data
        setRuns(Array.isArray(d) ? d : (d?.content ?? []))
      })
      .catch((err) => toastApiError(err, "Failed to load payroll runs."))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { loadRuns() }, [loadRuns])

  function handleProcess(run: PayrollRun) {
    setActionLoading(run.id)
    payrollService.processRun(run.id)
      .then((res) => {
        const updated: PayrollRun = (res.data as any)?.data ?? res.data
        setRuns((prev) => prev.map((r) => r.id === updated.id ? updated : r))
        toast.success(`${MONTH_NAMES[run.month - 1]} ${run.year} payroll processed.`)
      })
      .catch((err) => toastApiError(err, "Failed to process payroll run."))
      .finally(() => setActionLoading(null))
  }

  function handleApprove(run: PayrollRun) {
    setActionLoading(run.id)
    payrollService.approveRun(run.id)
      .then((res) => {
        const updated: PayrollRun = (res.data as any)?.data ?? res.data
        setRuns((prev) => prev.map((r) => r.id === updated.id ? updated : r))
        toast.success(`${MONTH_NAMES[run.month - 1]} ${run.year} payroll approved and PDFs generated.`)
      })
      .catch((err) => toastApiError(err, "Failed to approve payroll run."))
      .finally(() => setActionLoading(null))
  }

  if (activeRun) {
    return <PayslipsView run={activeRun} onBack={() => setActiveRun(null)} />
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Payroll"
        description="Manage payroll runs and employee salary structures."
        action={
          isPrivileged ? (
            <div className="flex gap-2">
              <Button variant="outline" onClick={() => setShowGenerate(true)} className="gap-1">
                <FileText className="h-4 w-4" />
                Generate Payslip
              </Button>
              {isHrAdmin && (
                <>
                  <Button variant="outline" onClick={() => setShowSetCtc(true)} className="gap-1">
                    <Wallet className="h-4 w-4" />
                    Set CTC
                  </Button>
                  <Button onClick={() => setShowNewRun(true)} className="gap-1">
                    <Plus className="h-4 w-4" />
                    New Run
                  </Button>
                </>
              )}
            </div>
          ) : undefined
        }
      />

      <Card>
        <CardContent className="p-0">
          {loading ? (
            <div className="space-y-3 p-6">
              {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-10 w-full" />)}
            </div>
          ) : runs.length === 0 ? (
            <EmptyState
              title="No payroll runs yet"
              description="Create your first payroll run to get started."
              action={
                isHrAdmin ? (
                  <Button onClick={() => setShowNewRun(true)} className="gap-1">
                    <Plus className="h-4 w-4" />New Payroll Run
                  </Button>
                ) : undefined
              }
            />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Period</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Employees</TableHead>
                  <TableHead className="text-right">Total Gross</TableHead>
                  <TableHead className="text-right">Deductions</TableHead>
                  <TableHead className="text-right">Net Pay</TableHead>
                  <TableHead>Run Date</TableHead>
                  <TableHead>Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {runs.map((run) => {
                  const isActing = actionLoading === run.id
                  return (
                    <TableRow key={run.id}>
                      <TableCell className="font-medium">
                        {MONTH_NAMES[run.month - 1]} {run.year}
                      </TableCell>
                      <TableCell>
                        <Badge variant={runBadge(run.status)}>{run.status}</Badge>
                      </TableCell>
                      <TableCell className="text-right">{run.totalEmployees}</TableCell>
                      <TableCell className="text-right">{inr(run.totalGross)}</TableCell>
                      <TableCell className="text-right">{inr(run.totalDeductions)}</TableCell>
                      <TableCell className="text-right font-semibold">{inr(run.totalNet)}</TableCell>
                      <TableCell className="text-sm text-muted-foreground">
                        {fmtDate(run.runDate)}
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          {isHrAdmin && run.status === "DRAFT" && (
                            <Button size="sm" variant="outline" disabled={isActing} onClick={() => handleProcess(run)}>
                              {isActing ? "Processing…" : "Process"}
                            </Button>
                          )}
                          {isHrAdmin && run.status === "PROCESSED" && (
                            <Button size="sm" variant="outline" disabled={isActing} onClick={() => handleApprove(run)}>
                              {isActing ? "Approving…" : "Approve"}
                            </Button>
                          )}
                          <Button size="sm" variant="ghost" onClick={() => setActiveRun(run)}>
                            View Payslips
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  )
                })}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <GeneratePayslipDialog open={showGenerate} onOpenChange={setShowGenerate} />
      <NewRunDialog
        open={showNewRun}
        onOpenChange={setShowNewRun}
        onCreated={(run) => setRuns((prev) => [run, ...prev])}
      />
      <SalaryStructureDialog open={showSetCtc} onOpenChange={setShowSetCtc} />
    </div>
  )
}

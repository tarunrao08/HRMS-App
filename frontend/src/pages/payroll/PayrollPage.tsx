import React, { useEffect, useState, useCallback, useRef } from "react"
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
import { Combobox } from "@/components/ui/combobox"
import ConfirmDialog from "@/components/shared/ConfirmDialog"
import { useFormSnapshot } from "@/hooks/useFormSnapshot"
import { useConfirmClose } from "@/hooks/useConfirmClose"
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
  type PayrollComponent, type CalculationType, type SalaryStructureComponentInput,
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

// Display-only relabels for catalog component names (backend data is unchanged).
const COMPONENT_DISPLAY_NAMES: Record<string, string> = {
  "Basic Salary": "Basic",
}

function displayComponentName(name: string): string {
  return COMPONENT_DISPLAY_NAMES[name] ?? name
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

// ── CTC breakdown preview (mirrors the two-pass resolution in
//    SalaryStructureServiceImpl#buildAndSaveStructure on the backend) ─────────

interface ComponentRow {
  payrollComponentId: string
  code: string
  name: string
  calculationType: CalculationType
  value: number | null
  percentageOfComponentId?: string | null
}

const RECONCILE_TOLERANCE = 0.1

function computeBreakdown(annualCtc: number, rows: ComponentRow[]) {
  const monthlyGross = annualCtc / 12
  const amounts: Record<string, number> = {}
  let formulaId: string | null = null

  // Pass 1: FIXED and gross-relative PERCENTAGE
  for (const r of rows) {
    if (r.calculationType === "FIXED") {
      amounts[r.payrollComponentId] = r.value || 0
    } else if (r.calculationType === "PERCENTAGE") {
      if (!r.percentageOfComponentId) {
        amounts[r.payrollComponentId] = monthlyGross * ((r.value || 0) / 100)
      }
    } else if (r.calculationType === "FORMULA") {
      formulaId = r.payrollComponentId
    }
  }

  // Pass 2: component-relative PERCENTAGE
  for (const r of rows) {
    if (r.calculationType === "PERCENTAGE" && r.percentageOfComponentId) {
      const ref = amounts[r.percentageOfComponentId]
      if (ref != null) amounts[r.payrollComponentId] = ref * ((r.value || 0) / 100)
    }
  }

  const sumOthers = Object.values(amounts).reduce((a, b) => a + b, 0)
  if (formulaId) {
    amounts[formulaId] = Math.max(monthlyGross - sumOthers, 0)
  }

  const total = Object.values(amounts).reduce((a, b) => a + b, 0)
  const reconciled = formulaId != null || Math.abs(total - monthlyGross) <= RECONCILE_TOLERANCE

  return { monthlyGross, amounts, total, reconciled }
}

// PF/ESI/PT constants — mirror SalaryStructureServiceImpl exactly, so the preview matches
// what the backend will actually save.
const PF_RATE        = 0.12
const PF_BASIC_CAP    = 15000
const PF_MAX_MONTHLY  = 1800
const ESI_EMP_RATE    = 0.0075
const ESI_EMPR_RATE   = 0.0325
const ESI_GROSS_LIMIT = 21000
const PT_THRESHOLD    = 10000
const PT_AMOUNT       = 200

function computeStatutory(monthlyGross: number, basicAmount: number) {
  const pfBase     = Math.min(basicAmount, PF_BASIC_CAP)
  const pfEmployee = Math.min(pfBase * PF_RATE, PF_MAX_MONTHLY)
  const pfEmployer = pfEmployee

  const esiApplicable = monthlyGross <= ESI_GROSS_LIMIT
  const esiEmployee = esiApplicable ? monthlyGross * ESI_EMP_RATE : 0
  const esiEmployer = esiApplicable ? monthlyGross * ESI_EMPR_RATE : 0

  const professionalTax = monthlyGross > PT_THRESHOLD ? PT_AMOUNT : 0

  const netSalary = monthlyGross - pfEmployee - esiEmployee - professionalTax

  return { pfEmployee, pfEmployer, esiApplicable, esiEmployee, esiEmployer, professionalTax, netSalary }
}

// Allowed %-of-basis ranges + industry-standard defaults for the common earning codes.
// Unlisted codes (custom catalog additions) are left unconstrained.
const COMPONENT_PCT_RANGES: Record<string, { min: number; max: number; default: number }> = {
  BASIC:      { min: 40, max: 50, default: 45 },   // % of monthly gross
  HRA:        { min: 30, max: 50, default: 40 },   // % of Basic
  DA:         { min: 5,  max: 10, default: 7.5 },  // % of Basic
  CONVEYANCE: { min: 5,  max: 10, default: 7.5 },  // % of Basic
  MEDICAL:    { min: 5,  max: 10, default: 7.5 },  // % of Basic
  TRANSPORT:  { min: 5,  max: 10, default: 7.5 },  // % of Basic
}

// ── Payslip detail dialog ─────────────────────────────────────────────────────

function PayslipDetailDialog({ payslip, onClose }: { payslip: Payslip; onClose: () => void }) {
  const [downloading, setDownloading] = useState(false)
  const title = `${payslip.employeeName} — ${MONTH_NAMES[payslip.month - 1]} ${payslip.year}`

  // A plain <a href="/api/..."> navigates the browser directly, which never attaches the
  // JWT bearer token (only axios's request interceptor does that) — the API then 403s with
  // an empty body, rendering as a blank tab. Fetch the PDF as an authenticated blob instead.
  async function handleDownloadPdf() {
    setDownloading(true)
    try {
      const res = await payrollService.downloadPayslipPdf(payslip.id)
      const url = URL.createObjectURL(new Blob([res.data as BlobPart], { type: "application/pdf" }))
      const a = document.createElement("a")
      a.href = url
      a.download = `payslip_${payslip.year}_${String(payslip.month).padStart(2, "0")}_${payslip.employeeCode}.pdf`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
    } catch (err) {
      toastApiError(err, "Failed to download payslip PDF.")
    } finally {
      setDownloading(false)
    }
  }

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
                {(payslip.components ?? []).length === 0 ? (
                  <tr><td className="py-1 text-muted-foreground" colSpan={2}>No components</td></tr>
                ) : (
                  payslip.components.map((c) => (
                    <tr key={c.payrollComponentId} className="border-b last:border-0">
                      <td className="py-1 text-muted-foreground">{displayComponentName(c.name)}</td>
                      <td className="py-1 text-right font-mono">{inr(c.amount)}</td>
                    </tr>
                  ))
                )}
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
            <Button variant="outline" size="sm" onClick={handleDownloadPdf} disabled={downloading}>
              <FileDown className="mr-2 h-4 w-4" />
              {downloading ? "Downloading…" : "Download PDF"}
            </Button>
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

  const { isDirty } = useFormSnapshot(open, { month, year })
  const { confirmOpen, setConfirmOpen, requestClose, confirmDiscard } =
    useConfirmClose(() => onOpenChange(false))

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
    <>
    <Dialog open={open} onOpenChange={(next) => { if (next) onOpenChange(true); else requestClose(isDirty()) }}>
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

// ── Salary Structure dialog ───────────────────────────────────────────────────

interface SalaryStructureDialogProps {
  open: boolean
  onOpenChange: (v: boolean) => void
}

type CtcBasis = "CTC" | "MONTHLY_GROSS" | "BASIC"

function SalaryStructureDialog({ open, onOpenChange }: SalaryStructureDialogProps) {
  const [employees, setEmployees] = useState<{ id: string; fullName: string; employeeCode: string }[]>([])
  const [employeeId, setEmployeeId]   = useState("NONE")
  const [basis, setBasis]             = useState<CtcBasis>("CTC")
  const [amountInput, setAmountInput] = useState("")
  const [effectiveFrom, setEffectiveFrom] = useState(
    new Date().toISOString().slice(0, 10)
  )
  const [saving, setSaving]           = useState(false)
  const [loadingExisting, setLoadingExisting] = useState(false)
  const [existing, setExisting]       = useState<SalaryStructureResponse | null>(null)
  const [catalog, setCatalog]         = useState<PayrollComponent[]>([])
  const [rows, setRows]               = useState<ComponentRow[]>([])

  // Re-baselined every time the catalog defaults or a selected employee's existing
  // structure finishes loading (see the two effects below) — "dirty" means edited
  // since that load, not merely different from whatever was on screen at dialog open.
  const snapshotRef = useRef({ amountInput: "", basis: "CTC" as CtcBasis, rows: [] as ComponentRow[] })
  function snapshot(next: { amountInput: string; basis: CtcBasis; rows: ComponentRow[] }) {
    snapshotRef.current = next
  }
  function isDirty(): boolean {
    return JSON.stringify({ amountInput, basis, rows })
      !== JSON.stringify(snapshotRef.current)
  }
  const { confirmOpen, setConfirmOpen, requestClose, confirmDiscard } =
    useConfirmClose(() => onOpenChange(false))

  function rowsFromCatalog(cat: PayrollComponent[]): ComponentRow[] {
    return cat.map((c) => ({
      payrollComponentId: c.id,
      code: c.code,
      name: c.name,
      calculationType: c.calculationType,
      value: c.value,
      percentageOfComponentId: c.percentageOfComponentId,
    }))
  }

  // Load the earning-component catalog once when the dialog opens
  useEffect(() => {
    if (!open) return
    payrollService.getActiveComponents()
      .then((res) => {
        const d = (res.data as any)?.data ?? res.data
        const earnings: PayrollComponent[] = (Array.isArray(d) ? d : [])
          .filter((c: PayrollComponent) => c.componentType === "EARNING")
          .sort((a: PayrollComponent, b: PayrollComponent) => a.displayOrder - b.displayOrder)
        setCatalog(earnings)
        const defaultRows = rowsFromCatalog(earnings)
        setRows(defaultRows)
        snapshot({ amountInput: "", basis: "CTC", rows: defaultRows })
      })
      .catch((err) => toastApiError(err, "Failed to load payroll components."))

    employeeService.getSummaries()
      .then((res) => {
        const d = (res.data as any)?.data ?? res.data
        setEmployees(Array.isArray(d) ? d : [])
      })
      .catch(() => {})
  }, [open])

  // Load current active structure when employee changes, and seed row overrides from it
  useEffect(() => {
    if (employeeId === "NONE" || catalog.length === 0) {
      setExisting(null)
      return
    }
    setLoadingExisting(true)
    payrollService.getActiveSalaryStructure(employeeId)
      .then((res) => {
        const d: SalaryStructureResponse | null = (res.data as any)?.data ?? res.data ?? null
        setExisting(d)
        setBasis("CTC")
        const nextAmount = d?.annualCtc ? String(d.annualCtc) : amountInput
        if (d?.annualCtc) setAmountInput(nextAmount)
        let nextRows = rows
        if (d?.components?.length) {
          const byId = new Map(d.components.map((c) => [c.payrollComponentId, c]))
          nextRows = catalog.map((c) => {
            const override = byId.get(c.id)
            return {
              payrollComponentId: c.id,
              code: c.code,
              name: c.name,
              calculationType: override?.calculationType ?? c.calculationType,
              value: override?.value ?? c.value,
              percentageOfComponentId: c.percentageOfComponentId,
            }
          })
          setRows(nextRows)
        }
        snapshot({ amountInput: nextAmount, basis: "CTC", rows: nextRows })
      })
      .catch(() => {
        setExisting(null)
        setBasis("CTC")
        setAmountInput("")
        // No existing structure — reset rows to catalog defaults
        const defaultRows = rowsFromCatalog(catalog)
        setRows(defaultRows)
        snapshot({ amountInput: "", basis: "CTC", rows: defaultRows })
      })
      .finally(() => setLoadingExisting(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [employeeId, catalog])

  // Basic drives the CTC <-> Basic conversion only when it's configured as a plain
  // percentage of monthly gross (the standard case). If HR has set Basic to FIXED or
  // chained it to another component, "enter CTC by Basic" can't be inverted client-side.
  const basicRow = rows.find((r) => r.code === "BASIC")
  const basicPctOfGross = (basicRow && basicRow.calculationType === "PERCENTAGE"
    && !basicRow.percentageOfComponentId && basicRow.value)
    ? basicRow.value / 100
    : null

  function annualCtcFrom(amt: number, b: CtcBasis): number {
    if (!amt || amt <= 0) return 0
    if (b === "CTC") return amt
    if (b === "MONTHLY_GROSS") return amt * 12
    // BASIC
    if (!basicPctOfGross) return 0
    return (amt / basicPctOfGross) * 12
  }

  const ctcNum = annualCtcFrom(parseFloat(amountInput) || 0, basis)
  const monthlyGrossNum = ctcNum > 0 ? ctcNum / 12 : 0

  function handleBasisChange(newBasis: CtcBasis) {
    const currentAnnual = ctcNum
    setBasis(newBasis)
    if (!currentAnnual) { setAmountInput(""); return }
    const monthlyGross = currentAnnual / 12
    if (newBasis === "CTC") setAmountInput(currentAnnual.toFixed(2))
    else if (newBasis === "MONTHLY_GROSS") setAmountInput(monthlyGross.toFixed(2))
    else setAmountInput(basicPctOfGross ? (monthlyGross * basicPctOfGross).toFixed(2) : "")
  }

  const preview = ctcNum > 0 && rows.length > 0 ? computeBreakdown(ctcNum, rows) : null
  const statutory = preview
    ? computeStatutory(preview.monthlyGross, basicRow ? (preview.amounts[basicRow.payrollComponentId] ?? 0) : 0)
    : null

  const invalidRows = rows.filter((r) => {
    const range = COMPONENT_PCT_RANGES[r.code]
    if (!range || r.calculationType !== "PERCENTAGE") return false
    return r.value == null || r.value < range.min || r.value > range.max
  })

  function updateRow(id: string, patch: Partial<ComponentRow>) {
    setRows((prev) => prev.map((r) => (r.payrollComponentId === id ? { ...r, ...patch } : r)))
  }

  function handleSave() {
    if (employeeId === "NONE" || !ctcNum || !effectiveFrom) {
      toast.error("Please fill all fields.")
      return
    }
    if (invalidRows.length > 0) {
      toast.error(`${invalidRows.map((r) => displayComponentName(r.name)).join(", ")} ${invalidRows.length > 1 ? "are" : "is"} outside the allowed % range.`)
      return
    }
    const components: SalaryStructureComponentInput[] = rows.map((r) => ({
      payrollComponentId: r.payrollComponentId,
      calculationType: r.calculationType,
      value: r.calculationType === "FORMULA" ? null : r.value,
    }))
    setSaving(true)
    payrollService.createSalaryStructure({ employeeId, annualCtc: ctcNum, effectiveFrom, components })
      .then(() => {
        toast.success("Salary structure saved.")
        onOpenChange(false)
        setAmountInput(""); setBasis("CTC"); setEmployeeId("NONE"); setExisting(null)
      })
      .catch((err) => toastApiError(err, "Failed to save salary structure."))
      .finally(() => setSaving(false))
  }

  const basisPlaceholder = basis === "CTC" ? "e.g. 600000 / year"
    : basis === "MONTHLY_GROSS" ? "e.g. 50000 / month"
    : "e.g. 22500 / month"

  return (
    <>
    <Dialog open={open} onOpenChange={(next) => { if (next) onOpenChange(true); else requestClose(isDirty()) }}>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader><DialogTitle>Set Employee CTC</DialogTitle></DialogHeader>

        <div className="space-y-4 max-h-[70vh] overflow-y-auto pr-1">
          {/* Employee picker */}
          <div className="space-y-1.5">
            <Label>Employee</Label>
            <Combobox
              options={employees.map((e) => ({ value: e.id, label: `${e.fullName} (${e.employeeCode})` }))}
              value={employeeId === "NONE" ? "" : employeeId}
              onChange={setEmployeeId}
              placeholder="Select employee"
              searchPlaceholder="Search employees…"
            />
            {existing && (
              <p className="text-xs text-muted-foreground">
                Current CTC: {inr(existing.annualCtc)}/yr (effective {existing.effectiveFrom})
              </p>
            )}
          </div>

          <div className="grid grid-cols-2 gap-3">
            {/* CTC input basis + amount */}
            <div className="space-y-1.5">
              <Label>Enter CTC By</Label>
              <div className="flex gap-2">
                <Select value={basis} onValueChange={(v) => handleBasisChange(v as CtcBasis)}>
                  <SelectTrigger className="w-[9.5rem]"><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="CTC">Annual CTC</SelectItem>
                    <SelectItem value="MONTHLY_GROSS">Monthly Gross</SelectItem>
                    <SelectItem value="BASIC">Monthly Basic</SelectItem>
                  </SelectContent>
                </Select>
                <Input type="number" min={0} step="0.01" placeholder={basisPlaceholder}
                  value={amountInput} onChange={(e) => setAmountInput(e.target.value)} />
              </div>
              {basis === "BASIC" && !basicPctOfGross && (
                <p className="text-xs text-destructive">
                  Basic isn't configured as a % of gross, so it can't be used to back into CTC.
                </p>
              )}
              {ctcNum > 0 && (
                <p className="text-xs text-muted-foreground">
                  ≈ {inr(ctcNum)}/yr CTC · {inr(monthlyGrossNum)}/mo gross
                </p>
              )}
            </div>

            {/* Effective From */}
            <div className="space-y-1.5">
              <Label>Effective From</Label>
              <Input type="date" value={effectiveFrom}
                onChange={(e) => setEffectiveFrom(e.target.value)} />
            </div>
          </div>

          {/* Salary component breakdown — fixed amount or variable % per component */}
          <div className="space-y-1.5">
            <Label>Salary Components</Label>
            <p className="text-xs text-muted-foreground -mt-1">
              Basic is a % of monthly gross; every other component here is a % of Basic (or a
              fixed ₹ amount). One component may be left as Formula to auto-absorb the remainder.
            </p>
            {rows.length === 0 ? (
              <p className="text-sm text-muted-foreground py-2">
                {loadingExisting ? "Loading…" : "No active earning components configured."}
              </p>
            ) : (
              <div className="rounded-lg border overflow-hidden">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Component</TableHead>
                      <TableHead className="w-32 text-center">Type</TableHead>
                      <TableHead className="w-32 text-center">Value</TableHead>
                      <TableHead className="w-28 text-center">Monthly ₹</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {rows.map((r) => {
                      const amount = preview?.amounts[r.payrollComponentId]
                      const range = COMPONENT_PCT_RANGES[r.code]
                      const isPct = r.calculationType === "PERCENTAGE"
                      const outOfRange = isPct && !!range
                        && (r.value == null || r.value < range.min || r.value > range.max)
                      const basisLabel = r.percentageOfComponentId
                        ? `% of ${displayComponentName(catalog.find((c) => c.id === r.percentageOfComponentId)?.name ?? "component")}`
                        : "% of Gross"
                      return (
                        <TableRow key={r.payrollComponentId}>
                          <TableCell className="font-medium">{displayComponentName(r.name)}</TableCell>
                          <TableCell>
                            <Select
                              value={r.calculationType}
                              onValueChange={(v) => updateRow(r.payrollComponentId, { calculationType: v as CalculationType })}
                            >
                              <SelectTrigger className="h-8 w-full"><SelectValue /></SelectTrigger>
                              <SelectContent>
                                <SelectItem value="FIXED">Fixed (₹)</SelectItem>
                                <SelectItem value="PERCENTAGE">Percentage</SelectItem>
                                <SelectItem value="FORMULA">Formula</SelectItem>
                              </SelectContent>
                            </Select>
                          </TableCell>
                          <TableCell className="text-right align-top">
                            {r.calculationType === "FORMULA" ? (
                              <div className="inline-flex items-center gap-1.5 h-8 px-2.5 bg-muted/50 border border-input rounded-md text-muted-foreground select-none float-right">
                                    {/* Auto Text */}
                                                                        <span className="text-xs font-medium">auto</span>
                                    {/* Lock Icon */}
                                    <svg xmlns="http://w3.org" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" className="w-3.5 h-3.5 text-muted-foreground/70">
                                      <path stroke-linecap="round" stroke-linejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 1 0-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z" />
                                    </svg>

                                  </div>
                            ) : (
                              <div>
                                <Input
                                  type="number" min={0} step="1"
                                  className={`h-8 text-right ${outOfRange ? "border-destructive focus-visible:ring-destructive" : ""}`}
                                  value={r.value ?? ""}
                                  onChange={(e) => updateRow(r.payrollComponentId, {
                                    value: e.target.value === "" ? null : parseFloat(e.target.value),
                                  })}
                                />
                                {isPct && (
                                  <p className={`mt-0.5 text-[10px] ${outOfRange ? "text-destructive" : "text-muted-foreground"}`}>
                                    {basisLabel}{range ? ` · ${range.min}–${range.max}%` : ""}
                                  </p>
                                )}
                              </div>
                            )}
                          </TableCell>
                          <TableCell className="text-right font-mono text-xs">
                            {amount != null ? inr(amount) : "—"}
                          </TableCell>
                        </TableRow>
                      )
                    })}
                  </TableBody>
                </Table>
              </div>
            )}
          </div>

          {/* Live breakdown preview: gross, components, and statutory deductions */}
          {preview && (
            <div className={`rounded-lg border p-3 space-y-2 ${preview.reconciled ? "bg-muted/30" : "border-destructive/50 bg-destructive/5"}`}>
              <div className="flex items-center justify-between text-sm">
                <span className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                  Monthly Gross
                </span>
                <span className="font-mono font-semibold">{inr(preview.monthlyGross)}</span>
              </div>
              <div className="flex items-center justify-between text-sm">
                <span className="text-xs text-muted-foreground">Sum of components</span>
                <span className="font-mono">{inr(preview.total)}</span>
              </div>
              {!preview.reconciled && (
                <p className="text-xs text-destructive">
                  Components don't add up to monthly gross. Adjust the percentages/amounts, or
                  set one component to Formula to auto-absorb the remainder.
                </p>
              )}
              {statutory && (
                <div className="border-t pt-2 space-y-1">
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-xs text-muted-foreground">PF (employee, 12% of Basic capped)</span>
                    <span className="font-mono">−{inr(statutory.pfEmployee)}</span>
                  </div>
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-xs text-muted-foreground">
                      ESI (employee) {!statutory.esiApplicable && "— N/A, gross > ₹21,000"}
                    </span>
                    <span className="font-mono">−{inr(statutory.esiEmployee)}</span>
                  </div>
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-xs text-muted-foreground">Professional Tax</span>
                    <span className="font-mono">−{inr(statutory.professionalTax)}</span>
                  </div>
                  <div className="flex items-center justify-between text-sm font-semibold pt-1">
                    <span>Net Salary (before TDS)</span>
                    <span className="font-mono">{inr(statutory.netSalary)}</span>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        <DialogFooter>
          <DialogClose asChild><Button variant="outline" disabled={saving}>Cancel</Button></DialogClose>
          <Button onClick={handleSave} disabled={saving || employeeId === "NONE" || invalidRows.length > 0}>
            {saving ? "Saving…" : "Save CTC"}
          </Button>
        </DialogFooter>
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

  const { isDirty: formIsDirty } = useFormSnapshot(open, { employeeId, month, year })
  // Once a payslip is generated there's nothing left to lose by closing.
  const isDirty = () => !generated && formIsDirty()

  function handleClose() {
    setEmployeeId("NONE")
    setMonth(String(currentMonth))
    setYear(String(currentYear))
    setGenerated(null)
    onOpenChange(false)
  }

  const { confirmOpen, setConfirmOpen, requestClose, confirmDiscard } = useConfirmClose(handleClose)

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
    <>
    <Dialog open={open} onOpenChange={(o) => { if (!o) requestClose(isDirty()) }}>
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
              <Combobox
                options={employees.map((e) => ({ value: e.id, label: `${e.fullName} (${e.employeeCode})` }))}
                value={employeeId === "NONE" ? "" : employeeId}
                onChange={setEmployeeId}
                placeholder="Select employee"
                searchPlaceholder="Search employees…"
              />
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

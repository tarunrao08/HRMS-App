import React, { useEffect, useState } from "react"
import toast from "react-hot-toast"
import { Pencil, Trash2, Plus, X } from "lucide-react"

import PageHeader from "@/components/shared/PageHeader"
import EmptyState from "@/components/shared/EmptyState"
import ConfirmDialog from "@/components/shared/ConfirmDialog"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Checkbox } from "@/components/ui/checkbox"
import { Badge } from "@/components/ui/badge"
import {
  Select, SelectTrigger, SelectValue, SelectContent, SelectItem,
} from "@/components/ui/select"
import {
  Table, TableHeader, TableBody, TableRow, TableHead, TableCell,
} from "@/components/ui/table"
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter,
} from "@/components/ui/dialog"

import leaveTypeService from "@/services/leaveTypeService"
import type {
  LeaveTypeResponse, ApplicableGender, LeaveTypeTenureTierResponse,
} from "@/services/leaveTypeService"
import { toastApiError } from "@/services/api"
import { useFormSnapshot } from "@/hooks/useFormSnapshot"
import { useConfirmClose } from "@/hooks/useConfirmClose"

interface FormState {
  name: string
  code: string
  description: string
  maxDaysPerYear: string
  carryForwardAllowed: boolean
  maxCarryForwardDays: string
  encashmentAllowed: boolean
  paid: boolean
  requiresDocument: boolean
  minNoticeDays: string
  active: boolean
  tenureBased: boolean
  applicableGender: ApplicableGender | ""
}

const emptyForm: FormState = {
  name: "",
  code: "",
  description: "",
  maxDaysPerYear: "0",
  carryForwardAllowed: false,
  maxCarryForwardDays: "0",
  encashmentAllowed: false,
  paid: true,
  requiresDocument: false,
  minNoticeDays: "0",
  active: true,
  tenureBased: false,
  applicableGender: "",
}

// ── Tier form state ───────────────────────────────────────────────────────────

interface TierFormState {
  minYears: string
  maxYears: string
  days: string
}

const emptyTierForm: TierFormState = { minYears: "", maxYears: "", days: "" }

function tierRangesOverlap(
  aMin: number, aMax: number | null, bMin: number, bMax: number | null,
): boolean {
  const effectiveAMax = aMax ?? Infinity
  const effectiveBMax = bMax ?? Infinity
  return aMin < effectiveBMax && bMin < effectiveAMax
}

// ── Tier Dialog ───────────────────────────────────────────────────────────────

interface TierDialogProps {
  open: boolean
  leaveTypeId: string
  existingTiers: LeaveTypeTenureTierResponse[]
  editingTier: LeaveTypeTenureTierResponse | null
  onClose: () => void
  onSuccess: () => void
}

function TierDialog({ open, leaveTypeId, existingTiers, editingTier, onClose, onSuccess }: TierDialogProps) {
  const [form, setForm] = useState<TierFormState>(emptyTierForm)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (!open) return
    setForm(editingTier
      ? {
          minYears: String(editingTier.minYears),
          maxYears: editingTier.maxYears != null ? String(editingTier.maxYears) : "",
          days: String(editingTier.days),
        }
      : emptyTierForm)
  }, [open, editingTier])

  const { isDirty } = useFormSnapshot(open, form)
  const { confirmOpen, setConfirmOpen, requestClose, confirmDiscard } = useConfirmClose(onClose)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const minYears = Number(form.minYears)
    const maxYears = form.maxYears.trim() === "" ? null : Number(form.maxYears)
    const days = Number(form.days)

    if (form.minYears.trim() === "" || Number.isNaN(minYears) || minYears < 0) {
      toast.error("Enter a valid minimum years (0 or more)")
      return
    }
    if (maxYears !== null && (Number.isNaN(maxYears) || maxYears <= minYears)) {
      toast.error("Max years must be greater than min years, or left blank for unbounded")
      return
    }
    if (form.days.trim() === "" || Number.isNaN(days) || days < 0) {
      toast.error("Enter a valid number of days")
      return
    }

    const overlap = existingTiers.find((t) => {
      if (editingTier && t.id === editingTier.id) return false
      return tierRangesOverlap(minYears, maxYears, t.minYears, t.maxYears ?? null)
    })
    if (overlap) {
      toast.error(`This range overlaps an existing tier (${overlap.minYears}–${overlap.maxYears ?? "∞"} years)`)
      return
    }

    setSubmitting(true)
    try {
      if (editingTier) {
        await leaveTypeService.updateTier(leaveTypeId, editingTier.id, { minYears, maxYears, days })
        toast.success("Tier updated successfully.")
      } else {
        await leaveTypeService.addTier(leaveTypeId, { minYears, maxYears, days })
        toast.success("Tier added successfully.")
      }
      onSuccess()
    } catch (err) {
      toastApiError(err, editingTier ? "Failed to update tier." : "Failed to add tier.")
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <>
    <Dialog open={open} onOpenChange={(o) => { if (o) return; if (!submitting) requestClose(isDirty()) }}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>{editingTier ? "Edit Tier" : "Add Tier"}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <Label>Min Years <span className="text-destructive">*</span></Label>
              <Input
                type="number"
                min={0}
                placeholder="e.g. 0"
                value={form.minYears}
                onChange={(e) => setForm((f) => ({ ...f, minYears: e.target.value }))}
                disabled={submitting}
              />
            </div>
            <div className="space-y-1.5">
              <Label>Max Years</Label>
              <Input
                type="number"
                min={0}
                placeholder="blank = unbounded"
                value={form.maxYears}
                onChange={(e) => setForm((f) => ({ ...f, maxYears: e.target.value }))}
                disabled={submitting}
              />
            </div>
          </div>
          <div className="space-y-1.5">
            <Label>Days Allocated <span className="text-destructive">*</span></Label>
            <Input
              type="number"
              min={0}
              value={form.days}
              onChange={(e) => setForm((f) => ({ ...f, days: e.target.value }))}
              disabled={submitting}
            />
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => requestClose(isDirty())} disabled={submitting}>Cancel</Button>
            <Button type="submit" disabled={submitting}>
              {submitting ? "Saving…" : editingTier ? "Save Changes" : "Add"}
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

// ── Main Page ─────────────────────────────────────────────────────────────────

export default function LeaveTypesPage() {
  const [leaveTypes, setLeaveTypes] = useState<LeaveTypeResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)

  const [dialogOpen, setDialogOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<LeaveTypeResponse | null>(null)
  const [form, setForm] = useState<FormState>(emptyForm)

  const [tiers, setTiers] = useState<LeaveTypeTenureTierResponse[]>([])
  const [loadingTiers, setLoadingTiers] = useState(false)
  const [tierDialogOpen, setTierDialogOpen] = useState(false)
  const [editingTier, setEditingTier] = useState<LeaveTypeTenureTierResponse | null>(null)
  const [deleteTierTarget, setDeleteTierTarget] = useState<LeaveTypeTenureTierResponse | null>(null)

  const [deleteTarget, setDeleteTarget] = useState<LeaveTypeResponse | null>(null)
  const [deleting, setDeleting] = useState(false)

  async function fetchData() {
    setLoading(true)
    try {
      const res = await leaveTypeService.getAll()
      const data = (res.data as any).data ?? res.data
      setLeaveTypes(Array.isArray(data) ? data : [])
    } catch (err) {
      toastApiError(err, "Failed to load leave types.")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchData() }, [])

  async function fetchTiers(leaveTypeId: string) {
    setLoadingTiers(true)
    try {
      const res = await leaveTypeService.getTiers(leaveTypeId)
      const data = (res.data as any).data ?? res.data
      setTiers(Array.isArray(data) ? data : [])
    } catch (err) {
      toastApiError(err, "Failed to load tenure tiers.")
    } finally {
      setLoadingTiers(false)
    }
  }

  function openAdd() {
    setEditTarget(null)
    setForm(emptyForm)
    setTiers([])
    setDialogOpen(true)
  }

  function openEdit(lt: LeaveTypeResponse) {
    setEditTarget(lt)
    setForm({
      name: lt.name,
      code: lt.code,
      description: lt.description ?? "",
      maxDaysPerYear: String(lt.maxDaysPerYear),
      carryForwardAllowed: lt.carryForwardAllowed,
      maxCarryForwardDays: String(lt.maxCarryForwardDays),
      encashmentAllowed: lt.encashmentAllowed,
      paid: lt.paid,
      requiresDocument: lt.requiresDocument,
      minNoticeDays: String(lt.minNoticeDays),
      active: lt.active,
      tenureBased: lt.tenureBased,
      applicableGender: lt.applicableGender ?? "",
    })
    setTiers([])
    if (lt.tenureBased) fetchTiers(lt.id)
    setDialogOpen(true)
  }

  const { isDirty } = useFormSnapshot(dialogOpen, form)
  const { confirmOpen, setConfirmOpen, requestClose, confirmDiscard } = useConfirmClose(() => {
    setDialogOpen(false)
    setEditTarget(null)
    setForm(emptyForm)
    setTiers([])
  })

  function handleDialogClose(open: boolean) {
    if (open) { setDialogOpen(true); return }
    if (!submitting) requestClose(isDirty())
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!form.name.trim()) { toast.error("Name is required."); return }
    if (!form.code.trim()) { toast.error("Code is required."); return }

    const payload = {
      name: form.name.trim(),
      code: form.code.trim().toUpperCase(),
      description: form.description.trim() || undefined,
      maxDaysPerYear: Number(form.maxDaysPerYear) || 0,
      carryForwardAllowed: form.carryForwardAllowed,
      maxCarryForwardDays: Number(form.maxCarryForwardDays) || 0,
      encashmentAllowed: form.encashmentAllowed,
      paid: form.paid,
      requiresDocument: form.requiresDocument,
      minNoticeDays: Number(form.minNoticeDays) || 0,
      active: form.active,
      tenureBased: form.tenureBased,
      applicableGender: form.applicableGender || undefined,
    }

    setSubmitting(true)
    try {
      if (editTarget) {
        await leaveTypeService.update(editTarget.id, payload)
        toast.success("Leave type updated successfully.")
      } else {
        await leaveTypeService.create(payload)
        toast.success("Leave type created successfully.")
      }
      setDialogOpen(false)
      setEditTarget(null)
      setForm(emptyForm)
      setTiers([])
      await fetchData()
    } catch (err) {
      toastApiError(err, editTarget ? "Failed to update leave type." : "Failed to create leave type.")
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return
    setDeleting(true)
    try {
      await leaveTypeService.delete(deleteTarget.id)
      toast.success("Leave type deleted successfully.")
      setDeleteTarget(null)
      await fetchData()
    } catch (err) {
      toastApiError(err, "Failed to delete leave type.")
    } finally {
      setDeleting(false)
    }
  }

  async function handleDeleteTier() {
    if (!deleteTierTarget || !editTarget) return
    try {
      await leaveTypeService.deleteTier(editTarget.id, deleteTierTarget.id)
      toast.success("Tier deleted successfully.")
      setDeleteTierTarget(null)
      await fetchTiers(editTarget.id)
    } catch (err) {
      toastApiError(err, "Failed to delete tier.")
    }
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Leave Types"
        description="Manage the master leave configuration every employee draws their balances from."
        action={
          <Button onClick={openAdd}>
            <Plus className="mr-2 h-4 w-4" />
            Add Leave Type
          </Button>
        }
      />

      <Card>
        <CardContent className="p-0">
          {!loading && leaveTypes.length === 0 ? (
            <EmptyState
              title="No leave types yet"
              description="Add your first leave type to get started."
              action={
                <Button onClick={openAdd}>
                  <Plus className="mr-2 h-4 w-4" />
                  Add Leave Type
                </Button>
              }
            />
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Name</TableHead>
                    <TableHead>Code</TableHead>
                    <TableHead className="text-center">Days/Year</TableHead>
                    <TableHead>Paid</TableHead>
                    <TableHead>Carry Fwd</TableHead>
                    <TableHead>Encashment</TableHead>
                    <TableHead>Tenure-based</TableHead>
                    <TableHead>Restricted To</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead className="text-right">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody className={loading ? "opacity-50 pointer-events-none" : ""}>
                  {loading ? (
                    <TableRow>
                      <TableCell colSpan={10} className="text-center text-muted-foreground py-8">
                        Loading…
                      </TableCell>
                    </TableRow>
                  ) : (
                    leaveTypes.map((lt) => (
                      <TableRow key={lt.id}>
                        <TableCell className="font-medium">{lt.name}</TableCell>
                        <TableCell className="font-mono text-xs">{lt.code}</TableCell>
                        <TableCell className="text-center">{lt.maxDaysPerYear}</TableCell>
                        <TableCell>
                          {lt.paid ? <Badge variant="success">Paid</Badge> : <Badge variant="secondary">Unpaid</Badge>}
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">
                          {lt.carryForwardAllowed ? `Up to ${lt.maxCarryForwardDays}d` : "—"}
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">
                          {lt.encashmentAllowed ? "Allowed" : "—"}
                        </TableCell>
                        <TableCell>
                          {lt.tenureBased ? <Badge variant="success">Tenure-based</Badge> : <span className="text-muted-foreground">—</span>}
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">
                          {lt.applicableGender ? lt.applicableGender.charAt(0) + lt.applicableGender.slice(1).toLowerCase() : "Everyone"}
                        </TableCell>
                        <TableCell>
                          {lt.active ? <Badge variant="success">Active</Badge> : <Badge variant="secondary">Inactive</Badge>}
                        </TableCell>
                        <TableCell className="text-right">
                          <div className="flex justify-end gap-2">
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => openEdit(lt)}
                              aria-label="Edit leave type"
                            >
                              <Pencil className="h-4 w-4" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => setDeleteTarget(lt)}
                              aria-label="Delete leave type"
                              className="text-destructive hover:text-destructive"
                            >
                              <Trash2 className="h-4 w-4" />
                            </Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Add / Edit Dialog */}
      <Dialog open={dialogOpen} onOpenChange={handleDialogClose}>
        <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{editTarget ? "Edit Leave Type" : "Add Leave Type"}</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="lt-name">Name <span className="text-destructive">*</span></Label>
                <Input
                  id="lt-name"
                  value={form.name}
                  onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                  placeholder="e.g. Casual Leave"
                  disabled={submitting}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="lt-code">Code <span className="text-destructive">*</span></Label>
                <Input
                  id="lt-code"
                  value={form.code}
                  onChange={(e) => setForm((f) => ({ ...f, code: e.target.value.toUpperCase() }))}
                  placeholder="e.g. CL"
                  maxLength={10}
                  disabled={submitting}
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="lt-description">Description</Label>
              <Textarea
                id="lt-description"
                value={form.description}
                onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
                placeholder="Optional description…"
                rows={2}
                disabled={submitting}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="lt-maxdays">
                  Max Days / Year {form.tenureBased && <span className="text-xs text-muted-foreground">(fallback if no tier matches)</span>}
                </Label>
                <Input
                  id="lt-maxdays"
                  type="number"
                  min={0}
                  value={form.maxDaysPerYear}
                  onChange={(e) => setForm((f) => ({ ...f, maxDaysPerYear: e.target.value }))}
                  disabled={submitting}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="lt-noticedays">Min Notice Days</Label>
                <Input
                  id="lt-noticedays"
                  type="number"
                  min={0}
                  value={form.minNoticeDays}
                  onChange={(e) => setForm((f) => ({ ...f, minNoticeDays: e.target.value }))}
                  disabled={submitting}
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <Label>Applicable Gender</Label>
              <Select
                value={form.applicableGender || "__all__"}
                onValueChange={(v) => setForm((f) => ({ ...f, applicableGender: v === "__all__" ? "" : v as ApplicableGender }))}
                disabled={submitting}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Everyone" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="__all__">Everyone</SelectItem>
                  <SelectItem value="MALE">Male only</SelectItem>
                  <SelectItem value="FEMALE">Female only</SelectItem>
                  <SelectItem value="OTHER">Other only</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="flex items-center gap-2">
              <Checkbox
                id="lt-paid"
                checked={form.paid}
                onCheckedChange={(checked) => setForm((f) => ({ ...f, paid: checked === true }))}
                disabled={submitting}
              />
              <Label htmlFor="lt-paid">Paid leave</Label>
            </div>

            <div className="flex items-center gap-2">
              <Checkbox
                id="lt-requires-doc"
                checked={form.requiresDocument}
                onCheckedChange={(checked) => setForm((f) => ({ ...f, requiresDocument: checked === true }))}
                disabled={submitting}
              />
              <Label htmlFor="lt-requires-doc">Requires supporting document</Label>
            </div>

            <div className="flex items-center gap-2">
              <Checkbox
                id="lt-carry-forward"
                checked={form.carryForwardAllowed}
                onCheckedChange={(checked) => setForm((f) => ({ ...f, carryForwardAllowed: checked === true }))}
                disabled={submitting}
              />
              <Label htmlFor="lt-carry-forward">Carry-forward allowed</Label>
            </div>

            {form.carryForwardAllowed && (
              <div className="space-y-1.5">
                <Label htmlFor="lt-max-carry">Max Carry-Forward Days</Label>
                <Input
                  id="lt-max-carry"
                  type="number"
                  min={0}
                  value={form.maxCarryForwardDays}
                  onChange={(e) => setForm((f) => ({ ...f, maxCarryForwardDays: e.target.value }))}
                  disabled={submitting}
                />
              </div>
            )}

            <div className="flex items-center gap-2">
              <Checkbox
                id="lt-encashment"
                checked={form.encashmentAllowed}
                onCheckedChange={(checked) => setForm((f) => ({ ...f, encashmentAllowed: checked === true }))}
                disabled={submitting}
              />
              <Label htmlFor="lt-encashment">Encashment allowed</Label>
            </div>

            <div className="flex items-center gap-2">
              <Checkbox
                id="lt-active"
                checked={form.active}
                onCheckedChange={(checked) => setForm((f) => ({ ...f, active: checked === true }))}
                disabled={submitting}
              />
              <Label htmlFor="lt-active">Active (available for new balance allocation)</Label>
            </div>

            <div className="flex items-center gap-2">
              <Checkbox
                id="lt-tenure-based"
                checked={form.tenureBased}
                onCheckedChange={(checked) => setForm((f) => ({ ...f, tenureBased: checked === true }))}
                disabled={submitting}
              />
              <Label htmlFor="lt-tenure-based">Tenure-based allocation (days scale with years of service)</Label>
            </div>

            {form.tenureBased && (
              <div className="rounded-md border p-3 space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium">Tenure Tiers</span>
                  {editTarget && (
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => { setEditingTier(null); setTierDialogOpen(true) }}
                    >
                      <Plus className="mr-1 h-3 w-3" />
                      Add Tier
                    </Button>
                  )}
                </div>

                {!editTarget ? (
                  <p className="text-xs text-muted-foreground">
                    Save this leave type first, then edit it to add tenure tiers (e.g. 0–1 yrs → 12 days,
                    1–3 yrs → 15, 3–5 yrs → 18, 5+ yrs → 21 — exact ranges and day counts are up to you).
                  </p>
                ) : loadingTiers ? (
                  <p className="text-xs text-muted-foreground">Loading tiers…</p>
                ) : tiers.length === 0 ? (
                  <p className="text-xs text-muted-foreground">
                    No tiers defined yet — until you add some, allocation falls back to Max Days/Year above.
                  </p>
                ) : (
                  <div className="space-y-1.5">
                    {tiers.map((t) => (
                      <div key={t.id} className="flex items-center justify-between rounded-sm bg-muted px-2 py-1.5 text-sm">
                        <span>{t.minYears}–{t.maxYears ?? "∞"} years → <span className="font-medium">{t.days} days</span></span>
                        <div className="flex gap-1">
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            onClick={() => { setEditingTier(t); setTierDialogOpen(true) }}
                            aria-label="Edit tier"
                          >
                            <Pencil className="h-3 w-3" />
                          </Button>
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            onClick={() => setDeleteTierTarget(t)}
                            aria-label="Delete tier"
                            className="text-destructive hover:text-destructive"
                          >
                            <X className="h-3 w-3" />
                          </Button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => handleDialogClose(false)}
                disabled={submitting}
              >
                Cancel
              </Button>
              <Button type="submit" disabled={submitting}>
                {submitting ? "Saving…" : editTarget ? "Save Changes" : "Create"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Tier Add/Edit Dialog */}
      {editTarget && (
        <TierDialog
          open={tierDialogOpen}
          leaveTypeId={editTarget.id}
          existingTiers={tiers}
          editingTier={editingTier}
          onClose={() => setTierDialogOpen(false)}
          onSuccess={() => { setTierDialogOpen(false); fetchTiers(editTarget.id) }}
        />
      )}

      {/* Cancel Confirm */}
      <ConfirmDialog
        open={confirmOpen}
        onOpenChange={setConfirmOpen}
        title="Discard changes?"
        description="You have unsaved changes in this form. Are you sure you want to cancel? Your changes will be lost."
        confirmLabel="Discard"
        cancelLabel="Keep Editing"
        onConfirm={confirmDiscard}
      />

      {/* Delete Tier Confirm */}
      <ConfirmDialog
        open={!!deleteTierTarget}
        onOpenChange={(open) => { if (!open) setDeleteTierTarget(null) }}
        title="Delete Tier"
        description={deleteTierTarget
          ? `Delete the ${deleteTierTarget.minYears}–${deleteTierTarget.maxYears ?? "∞"} years tier? This does not affect balances already allocated.`
          : undefined}
        confirmLabel="Delete"
        variant="destructive"
        onConfirm={handleDeleteTier}
      />

      {/* Delete Confirm Dialog */}
      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={(open) => { if (!open) setDeleteTarget(null) }}
        title="Delete Leave Type"
        description={`Are you sure you want to delete "${deleteTarget?.name}"? This action cannot be undone.`}
        confirmLabel="Delete"
        variant="destructive"
        loading={deleting}
        onConfirm={handleDelete}
      />
    </div>
  )
}

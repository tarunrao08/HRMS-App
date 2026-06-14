import React, { useEffect, useState } from "react"
import toast from "react-hot-toast"
import { Pencil, Trash2, Plus } from "lucide-react"

import PageHeader from "@/components/shared/PageHeader"
import EmptyState from "@/components/shared/EmptyState"
import ConfirmDialog from "@/components/shared/ConfirmDialog"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import {
  Table, TableHeader, TableBody, TableRow, TableHead, TableCell,
} from "@/components/ui/table"
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter,
} from "@/components/ui/dialog"

import branchService, { BranchResponse } from "@/services/branchService"
import { toastApiError } from "@/services/api"

interface FormState {
  name: string
  city: string
  state: string
  country: string
  address: string
}

const emptyForm: FormState = { name: "", city: "", state: "", country: "", address: "" }

export default function BranchesPage() {
  const [branches, setBranches] = useState<BranchResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)

  const [dialogOpen, setDialogOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<BranchResponse | null>(null)
  const [form, setForm] = useState<FormState>(emptyForm)

  const [deleteTarget, setDeleteTarget] = useState<BranchResponse | null>(null)
  const [deleting, setDeleting] = useState(false)

  async function fetchBranches() {
    setLoading(true)
    try {
      const res = await branchService.getAll()
      const data = (res.data as any).data ?? res.data
      setBranches(Array.isArray(data) ? data : [])
    } catch (err) {
      toastApiError(err, "Failed to load branches.")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchBranches() }, [])

  function openAdd() {
    setEditTarget(null)
    setForm(emptyForm)
    setDialogOpen(true)
  }

  function openEdit(branch: BranchResponse) {
    setEditTarget(branch)
    setForm({
      name: branch.name,
      city: branch.city,
      state: branch.state,
      country: branch.country,
      address: branch.address ?? "",
    })
    setDialogOpen(true)
  }

  function handleDialogClose(open: boolean) {
    if (!open && !submitting) {
      setDialogOpen(false)
      setEditTarget(null)
      setForm(emptyForm)
    }
  }

  function setField(field: keyof FormState) {
    return (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
      setForm((f) => ({ ...f, [field]: e.target.value }))
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!form.name.trim()) { toast.error("Branch name is required."); return }
    if (!form.city.trim()) { toast.error("City is required."); return }
    if (!form.state.trim()) { toast.error("State is required."); return }
    if (!form.country.trim()) { toast.error("Country is required."); return }

    setSubmitting(true)
    try {
      const payload = {
        name: form.name.trim(),
        city: form.city.trim(),
        state: form.state.trim(),
        country: form.country.trim(),
        address: form.address.trim() || undefined,
      }
      if (editTarget) {
        await branchService.update(editTarget.id, payload)
        toast.success("Branch updated successfully.")
      } else {
        await branchService.create(payload)
        toast.success("Branch created successfully.")
      }
      setDialogOpen(false)
      setEditTarget(null)
      setForm(emptyForm)
      await fetchBranches()
    } catch (err) {
      toastApiError(err, editTarget ? "Failed to update branch." : "Failed to create branch.")
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return
    setDeleting(true)
    try {
      await branchService.delete(deleteTarget.id)
      toast.success("Branch deleted successfully.")
      setDeleteTarget(null)
      await fetchBranches()
    } catch (err) {
      toastApiError(err, "Failed to delete branch.")
    } finally {
      setDeleting(false)
    }
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Branches"
        description="Manage your organisation's office locations."
        action={
          <Button onClick={openAdd}>
            <Plus className="mr-2 h-4 w-4" />
            Add Branch
          </Button>
        }
      />

      <Card>
        <CardContent className="p-0">
          {!loading && branches.length === 0 ? (
            <EmptyState
              title="No branches yet"
              description="Add your first branch location to get started."
              action={
                <Button onClick={openAdd}>
                  <Plus className="mr-2 h-4 w-4" />
                  Add Branch
                </Button>
              }
            />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>City</TableHead>
                  <TableHead>State</TableHead>
                  <TableHead>Country</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody className={loading ? "opacity-50 pointer-events-none" : ""}>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={5} className="text-center text-muted-foreground py-8">
                      Loading…
                    </TableCell>
                  </TableRow>
                ) : (
                  branches.map((branch) => (
                    <TableRow key={branch.id}>
                      <TableCell className="font-medium">{branch.name}</TableCell>
                      <TableCell>{branch.city}</TableCell>
                      <TableCell>{branch.state}</TableCell>
                      <TableCell>{branch.country}</TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-2">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => openEdit(branch)}
                            aria-label="Edit branch"
                          >
                            <Pencil className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setDeleteTarget(branch)}
                            aria-label="Delete branch"
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
          )}
        </CardContent>
      </Card>

      {/* Add / Edit Dialog */}
      <Dialog open={dialogOpen} onOpenChange={handleDialogClose}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>{editTarget ? "Edit Branch" : "Add Branch"}</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="branch-name">
                Name <span className="text-destructive">*</span>
              </Label>
              <Input
                id="branch-name"
                value={form.name}
                onChange={setField("name")}
                placeholder="e.g. Mumbai HQ"
                disabled={submitting}
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="branch-city">
                  City <span className="text-destructive">*</span>
                </Label>
                <Input
                  id="branch-city"
                  value={form.city}
                  onChange={setField("city")}
                  placeholder="e.g. Mumbai"
                  disabled={submitting}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="branch-state">
                  State <span className="text-destructive">*</span>
                </Label>
                <Input
                  id="branch-state"
                  value={form.state}
                  onChange={setField("state")}
                  placeholder="e.g. Maharashtra"
                  disabled={submitting}
                />
              </div>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="branch-country">
                Country <span className="text-destructive">*</span>
              </Label>
              <Input
                id="branch-country"
                value={form.country}
                onChange={setField("country")}
                placeholder="e.g. India"
                disabled={submitting}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="branch-address">Address</Label>
              <Textarea
                id="branch-address"
                value={form.address}
                onChange={setField("address")}
                placeholder="Street address, landmark…"
                rows={3}
                disabled={submitting}
              />
            </div>
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

      {/* Delete Confirm Dialog */}
      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={(open) => { if (!open) setDeleteTarget(null) }}
        title="Delete Branch"
        description={`Are you sure you want to delete "${deleteTarget?.name}"? This action cannot be undone.`}
        confirmLabel="Delete"
        variant="destructive"
        loading={deleting}
        onConfirm={handleDelete}
      />
    </div>
  )
}

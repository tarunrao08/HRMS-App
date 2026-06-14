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
import {
  Select, SelectTrigger, SelectValue, SelectContent, SelectItem,
} from "@/components/ui/select"

import designationService, { DesignationResponse } from "@/services/designationService"
import departmentService, { DepartmentResponse } from "@/services/departmentService"
import { toastApiError } from "@/services/api"

interface FormState {
  name: string
  departmentId: string
  description: string
}

const emptyForm: FormState = { name: "", departmentId: "", description: "" }

export default function DesignationsPage() {
  const [designations, setDesignations] = useState<DesignationResponse[]>([])
  const [departments, setDepartments] = useState<DepartmentResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)

  const [dialogOpen, setDialogOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<DesignationResponse | null>(null)
  const [form, setForm] = useState<FormState>(emptyForm)

  const [deleteTarget, setDeleteTarget] = useState<DesignationResponse | null>(null)
  const [deleting, setDeleting] = useState(false)

  async function fetchData() {
    setLoading(true)
    try {
      const [desigRes, deptRes] = await Promise.all([
        designationService.getAll(),
        departmentService.getAll(),
      ])
      const desigData = (desigRes.data as any).data ?? desigRes.data
      const deptData  = (deptRes.data  as any).data ?? deptRes.data
      setDesignations(Array.isArray(desigData) ? desigData : [])
      setDepartments(Array.isArray(deptData)   ? deptData  : [])
    } catch (err) {
      toastApiError(err, "Failed to load designations.")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchData() }, [])

  function openAdd() {
    setEditTarget(null)
    setForm(emptyForm)
    setDialogOpen(true)
  }

  function openEdit(desig: DesignationResponse) {
    setEditTarget(desig)
    setForm({
      name: desig.name,
      departmentId: desig.departmentId,
      description: desig.description ?? "",
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

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!form.name.trim()) {
      toast.error("Designation name is required.")
      return
    }
    if (!form.departmentId) {
      toast.error("Please select a department.")
      return
    }
    setSubmitting(true)
    try {
      if (editTarget) {
        await designationService.update(editTarget.id, {
          name: form.name.trim(),
          departmentId: form.departmentId,
          description: form.description.trim() || undefined,
        })
        toast.success("Designation updated successfully.")
      } else {
        await designationService.create({
          name: form.name.trim(),
          departmentId: form.departmentId,
          description: form.description.trim() || undefined,
        })
        toast.success("Designation created successfully.")
      }
      setDialogOpen(false)
      setEditTarget(null)
      setForm(emptyForm)
      await fetchData()
    } catch (err) {
      toastApiError(err, editTarget ? "Failed to update designation." : "Failed to create designation.")
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return
    setDeleting(true)
    try {
      await designationService.delete(deleteTarget.id)
      toast.success("Designation deleted successfully.")
      setDeleteTarget(null)
      await fetchData()
    } catch (err) {
      toastApiError(err, "Failed to delete designation.")
    } finally {
      setDeleting(false)
    }
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Designations"
        description="Manage job designations within your departments."
        action={
          <Button onClick={openAdd}>
            <Plus className="mr-2 h-4 w-4" />
            Add Designation
          </Button>
        }
      />

      <Card>
        <CardContent className="p-0">
          {!loading && designations.length === 0 ? (
            <EmptyState
              title="No designations yet"
              description="Add your first designation to get started."
              action={
                <Button onClick={openAdd}>
                  <Plus className="mr-2 h-4 w-4" />
                  Add Designation
                </Button>
              }
            />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>Department</TableHead>
                  <TableHead>Description</TableHead>
                  <TableHead>Created At</TableHead>
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
                  designations.map((desig) => (
                    <TableRow key={desig.id}>
                      <TableCell className="font-medium">{desig.name}</TableCell>
                      <TableCell>{desig.departmentName}</TableCell>
                      <TableCell className="text-muted-foreground">
                        {desig.description ?? "—"}
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {new Date(desig.createdAt).toLocaleDateString()}
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-2">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => openEdit(desig)}
                            aria-label="Edit designation"
                          >
                            <Pencil className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setDeleteTarget(desig)}
                            aria-label="Delete designation"
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
            <DialogTitle>{editTarget ? "Edit Designation" : "Add Designation"}</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="desig-name">
                Name <span className="text-destructive">*</span>
              </Label>
              <Input
                id="desig-name"
                value={form.name}
                onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                placeholder="e.g. Senior Engineer"
                disabled={submitting}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="desig-department">
                Department <span className="text-destructive">*</span>
              </Label>
              <Select
                value={form.departmentId}
                onValueChange={(val) => setForm((f) => ({ ...f, departmentId: val }))}
                disabled={submitting}
              >
                <SelectTrigger id="desig-department">
                  <SelectValue placeholder="Select a department" />
                </SelectTrigger>
                <SelectContent>
                  {departments.map((dept) => (
                    <SelectItem key={dept.id} value={dept.id}>
                      {dept.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="desig-description">Description</Label>
              <Textarea
                id="desig-description"
                value={form.description}
                onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
                placeholder="Optional description…"
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
        title="Delete Designation"
        description={`Are you sure you want to delete "${deleteTarget?.name}"? This action cannot be undone.`}
        confirmLabel="Delete"
        variant="destructive"
        loading={deleting}
        onConfirm={handleDelete}
      />
    </div>
  )
}

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

import departmentService, { DepartmentResponse } from "@/services/departmentService"
import { toastApiError } from "@/services/api"

interface FormState {
  name: string
  description: string
}

const emptyForm: FormState = { name: "", description: "" }

export default function DepartmentsPage() {
  const [departments, setDepartments] = useState<DepartmentResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)

  const [dialogOpen, setDialogOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<DepartmentResponse | null>(null)
  const [form, setForm] = useState<FormState>(emptyForm)

  const [deleteTarget, setDeleteTarget] = useState<DepartmentResponse | null>(null)
  const [deleting, setDeleting] = useState(false)

  async function fetchDepartments() {
    setLoading(true)
    try {
      const res = await departmentService.getAll()
      const data = (res.data as any).data ?? res.data
      setDepartments(Array.isArray(data) ? data : [])
    } catch (err) {
      toastApiError(err, "Failed to load departments.")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchDepartments() }, [])

  function openAdd() {
    setEditTarget(null)
    setForm(emptyForm)
    setDialogOpen(true)
  }

  function openEdit(dept: DepartmentResponse) {
    setEditTarget(dept)
    setForm({ name: dept.name, description: dept.description ?? "" })
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
      toast.error("Department name is required.")
      return
    }
    setSubmitting(true)
    try {
      if (editTarget) {
        await departmentService.update(editTarget.id, {
          name: form.name.trim(),
          description: form.description.trim() || undefined,
        })
        toast.success("Department updated successfully.")
      } else {
        await departmentService.create({
          name: form.name.trim(),
          description: form.description.trim() || undefined,
        })
        toast.success("Department created successfully.")
      }
      setDialogOpen(false)
      setEditTarget(null)
      setForm(emptyForm)
      await fetchDepartments()
    } catch (err) {
      toastApiError(err, editTarget ? "Failed to update department." : "Failed to create department.")
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return
    setDeleting(true)
    try {
      await departmentService.delete(deleteTarget.id)
      toast.success("Department deleted successfully.")
      setDeleteTarget(null)
      await fetchDepartments()
    } catch (err) {
      toastApiError(err, "Failed to delete department.")
    } finally {
      setDeleting(false)
    }
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Departments"
        description="Manage your organisation's departments."
        action={
          <Button onClick={openAdd}>
            <Plus className="mr-2 h-4 w-4" />
            Add Department
          </Button>
        }
      />

      <Card>
        <CardContent className="p-0">
          {!loading && departments.length === 0 ? (
            <EmptyState
              title="No departments yet"
              description="Add your first department to get started."
              action={
                <Button onClick={openAdd}>
                  <Plus className="mr-2 h-4 w-4" />
                  Add Department
                </Button>
              }
            />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>Description</TableHead>
                  <TableHead>Created At</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody className={loading ? "opacity-50 pointer-events-none" : ""}>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={4} className="text-center text-muted-foreground py-8">
                      Loading…
                    </TableCell>
                  </TableRow>
                ) : (
                  departments.map((dept) => (
                    <TableRow key={dept.id}>
                      <TableCell className="font-medium">{dept.name}</TableCell>
                      <TableCell className="text-muted-foreground">
                        {dept.description ?? "—"}
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {new Date(dept.createdAt).toLocaleDateString()}
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-2">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => openEdit(dept)}
                            aria-label="Edit department"
                          >
                            <Pencil className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setDeleteTarget(dept)}
                            aria-label="Delete department"
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
            <DialogTitle>{editTarget ? "Edit Department" : "Add Department"}</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="dept-name">
                Name <span className="text-destructive">*</span>
              </Label>
              <Input
                id="dept-name"
                value={form.name}
                onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                placeholder="e.g. Engineering"
                disabled={submitting}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="dept-description">Description</Label>
              <Textarea
                id="dept-description"
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
        title="Delete Department"
        description={`Are you sure you want to delete "${deleteTarget?.name}"? This action cannot be undone.`}
        confirmLabel="Delete"
        variant="destructive"
        loading={deleting}
        onConfirm={handleDelete}
      />
    </div>
  )
}

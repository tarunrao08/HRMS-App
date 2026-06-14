import React, { useEffect, useState } from "react"
import toast from "react-hot-toast"
import { Plus, ShieldCheck } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Table, TableHeader, TableBody, TableRow, TableHead, TableCell,
} from "@/components/ui/table"
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogClose,
} from "@/components/ui/dialog"
import {
  Select, SelectTrigger, SelectValue, SelectContent, SelectItem,
} from "@/components/ui/select"
import PageHeader from "@/components/shared/PageHeader"
import EmptyState from "@/components/shared/EmptyState"
import userService, { type UserAccount } from "@/services/userService"
import employeeService from "@/services/employeeService"
import { toastApiError } from "@/services/api"

// ── Helpers ───────────────────────────────────────────────────────────────────

function roleLabel(role: string) {
  switch (role) {
    case "ROLE_HR_ADMIN": return "HR Admin"
    case "ROLE_MANAGER":  return "Manager"
    case "ROLE_EMPLOYEE": return "Employee"
    default: return role
  }
}

function roleBadgeVariant(role: string): "default" | "warning" | "success" {
  if (role === "ROLE_HR_ADMIN") return "default"
  if (role === "ROLE_MANAGER")  return "warning"
  return "success"
}

function formatDate(iso?: string) {
  if (!iso) return "—"
  return new Date(iso).toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" })
}

// ── Create Account dialog ─────────────────────────────────────────────────────

interface EmployeeSummary { id: string; employeeCode: string; fullName: string }

interface CreateDialogProps {
  open: boolean
  onOpenChange: (v: boolean) => void
  onCreated: (u: UserAccount) => void
}

function CreateAccountDialog({ open, onOpenChange, onCreated }: CreateDialogProps) {
  const [employees, setEmployees]   = useState<EmployeeSummary[]>([])
  const [employeeId, setEmployeeId] = useState("")
  const [username, setUsername]     = useState("")
  const [password, setPassword]     = useState("")
  const [role, setRole]             = useState("ROLE_EMPLOYEE")
  const [saving, setSaving]         = useState(false)
  const [loadingEmps, setLoadingEmps] = useState(false)

  useEffect(() => {
    if (!open) return
    setLoadingEmps(true)
    employeeService.getSummaries()
      .then((r) => {
        const data = (r.data as any)?.data ?? r.data
        setEmployees(Array.isArray(data) ? data : [])
      })
      .catch(() => setEmployees([]))
      .finally(() => setLoadingEmps(false))
  }, [open])

  useEffect(() => {
    if (!open) {
      setEmployeeId(""); setUsername(""); setPassword(""); setRole("ROLE_EMPLOYEE")
    }
  }, [open])

  function handleEmployeeChange(id: string) {
    setEmployeeId(id)
    const emp = employees.find((e) => e.id === id)
    if (emp) {
      const parts = emp.fullName.toLowerCase().split(" ")
      setUsername(parts.filter(Boolean).join("."))
    }
  }

  function handleSubmit() {
    if (!employeeId || !username.trim() || !password.trim()) {
      toast.error("Please fill in all required fields.")
      return
    }
    setSaving(true)
    userService
      .createUser({ employeeId, username: username.trim(), password: password.trim(), role })
      .then((r) => {
        const account: UserAccount = (r.data as any)?.data ?? r.data
        toast.success(`Account created — Username: ${account.username}`)
        onCreated(account)
        onOpenChange(false)
      })
      .catch((err) => toastApiError(err, "Failed to create account."))
      .finally(() => setSaving(false))
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>Create Login Account</DialogTitle>
        </DialogHeader>

        <div className="space-y-4 py-1">
          <div className="space-y-1.5">
            <Label>Employee <span className="text-destructive">*</span></Label>
            <Select value={employeeId} onValueChange={handleEmployeeChange} disabled={loadingEmps}>
              <SelectTrigger>
                <SelectValue placeholder={loadingEmps ? "Loading…" : "Select employee"} />
              </SelectTrigger>
              <SelectContent>
                {employees.map((e) => (
                  <SelectItem key={e.id} value={e.id}>
                    {e.fullName} ({e.employeeCode})
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="username">Username <span className="text-destructive">*</span></Label>
            <Input
              id="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="e.g. john.doe"
            />
            <p className="text-xs text-muted-foreground">Auto-suggested from employee name — you can edit it.</p>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="password">Initial Password <span className="text-destructive">*</span></Label>
            <Input
              id="password"
              type="text"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Min. 6 characters"
            />
            <p className="text-xs text-muted-foreground">Share this with the employee so they can log in for the first time.</p>
          </div>

          <div className="space-y-1.5">
            <Label>Role <span className="text-destructive">*</span></Label>
            <Select value={role} onValueChange={setRole}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ROLE_EMPLOYEE">Employee</SelectItem>
                <SelectItem value="ROLE_MANAGER">Manager</SelectItem>
                <SelectItem value="ROLE_HR_ADMIN">HR Admin</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>

        <DialogFooter>
          <DialogClose asChild>
            <Button variant="outline" disabled={saving}>Cancel</Button>
          </DialogClose>
          <Button onClick={handleSubmit} disabled={saving || loadingEmps}>
            {saving ? "Creating…" : "Create Account"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

// ── Main Page ─────────────────────────────────────────────────────────────────

export default function UserAccountsPage() {
  const [users, setUsers]           = useState<UserAccount[]>([])
  const [loading, setLoading]       = useState(true)
  const [showCreate, setShowCreate] = useState(false)

  function loadUsers() {
    setLoading(true)
    userService.getAll()
      .then((r) => {
        const data = (r.data as any)?.data ?? r.data
        setUsers(Array.isArray(data) ? data : [])
      })
      .catch(() => setUsers([]))
      .finally(() => setLoading(false))
  }

  useEffect(() => { loadUsers() }, [])

  return (
    <div className="space-y-4">
      <PageHeader
        title="User Accounts"
        description="Manage employee login credentials and roles."
        action={
          <Button onClick={() => setShowCreate(true)} className="gap-1">
            <Plus className="h-4 w-4" />
            Create Account
          </Button>
        }
      />

      <div className="rounded-md border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Username</TableHead>
              <TableHead>Employee</TableHead>
              <TableHead>Email</TableHead>
              <TableHead>Role</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Last Login</TableHead>
              <TableHead>Created</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {loading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <TableRow key={i}>
                  {Array.from({ length: 7 }).map((__, j) => (
                    <TableCell key={j}><Skeleton className="h-4 w-full" /></TableCell>
                  ))}
                </TableRow>
              ))
            ) : users.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} className="p-0">
                  <EmptyState
                    title="No user accounts yet"
                    description="Create an account so employees can log in."
                    action={
                      <Button onClick={() => setShowCreate(true)} className="gap-1">
                        <Plus className="h-4 w-4" />
                        Create Account
                      </Button>
                    }
                  />
                </TableCell>
              </TableRow>
            ) : (
              users.map((u) => (
                <TableRow key={u.id}>
                  <TableCell>
                    <div className="flex items-center gap-1.5 font-medium text-sm">
                      <ShieldCheck className="h-3.5 w-3.5 text-muted-foreground" />
                      {u.username}
                    </div>
                  </TableCell>
                  <TableCell>
                    {u.employeeName ? (
                      <div>
                        <p className="text-sm font-medium">{u.employeeName}</p>
                        <p className="text-xs text-muted-foreground">{u.employeeCode}</p>
                      </div>
                    ) : (
                      <span className="text-muted-foreground text-sm">System</span>
                    )}
                  </TableCell>
                  <TableCell className="text-sm">{u.email}</TableCell>
                  <TableCell>
                    <div className="flex flex-wrap gap-1">
                      {u.roles.map((r) => (
                        <Badge key={r} variant={roleBadgeVariant(r)} className="text-xs">
                          {roleLabel(r)}
                        </Badge>
                      ))}
                    </div>
                  </TableCell>
                  <TableCell>
                    <Badge variant={u.enabled ? "success" : "destructive"}>
                      {u.enabled ? "Active" : "Disabled"}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">{formatDate(u.lastLoginAt)}</TableCell>
                  <TableCell className="text-sm text-muted-foreground">{formatDate(u.createdAt)}</TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      <CreateAccountDialog
        open={showCreate}
        onOpenChange={setShowCreate}
        onCreated={(u) => setUsers((prev) => [u, ...prev])}
      />
    </div>
  )
}

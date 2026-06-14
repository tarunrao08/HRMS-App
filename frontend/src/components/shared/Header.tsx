import { useState, useRef, useEffect } from "react"
import { useNavigate } from "react-router-dom"
import { LogOut, KeyRound, ChevronDown } from "lucide-react"
import toast from "react-hot-toast"
import { useAuthStore } from "@/store/authStore"
import { authService } from "@/services/authService"
import { toastApiError } from "@/services/api"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogClose,
} from "@/components/ui/dialog"

// ── Change Password Dialog ────────────────────────────────────────────────────

function ChangePasswordDialog({
  open,
  onOpenChange,
}: {
  open: boolean
  onOpenChange: (v: boolean) => void
}) {
  const [currentPassword, setCurrentPassword] = useState("")
  const [newPassword, setNewPassword]         = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const [saving, setSaving]                   = useState(false)

  useEffect(() => {
    if (!open) {
      setCurrentPassword("")
      setNewPassword("")
      setConfirmPassword("")
    }
  }, [open])

  async function handleSubmit() {
    if (!currentPassword || !newPassword || !confirmPassword) {
      toast.error("Please fill in all fields.")
      return
    }
    if (newPassword.length < 6) {
      toast.error("New password must be at least 6 characters.")
      return
    }
    if (newPassword !== confirmPassword) {
      toast.error("New password and confirmation do not match.")
      return
    }
    setSaving(true)
    try {
      await authService.changePassword(currentPassword, newPassword)
      toast.success("Password changed successfully.")
      onOpenChange(false)
    } catch (err) {
      toastApiError(err, "Failed to change password.")
    } finally {
      setSaving(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>Change Password</DialogTitle>
        </DialogHeader>

        <div className="space-y-4 py-1">
          <div className="space-y-1.5">
            <Label htmlFor="curr-pw">Current Password</Label>
            <Input
              id="curr-pw"
              type="password"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              placeholder="Enter current password"
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="new-pw">New Password</Label>
            <Input
              id="new-pw"
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder="Min. 6 characters"
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="conf-pw">Confirm New Password</Label>
            <Input
              id="conf-pw"
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="Re-enter new password"
            />
          </div>
        </div>

        <DialogFooter>
          <DialogClose asChild>
            <Button variant="outline" disabled={saving}>Cancel</Button>
          </DialogClose>
          <Button onClick={handleSubmit} disabled={saving}>
            {saving ? "Saving…" : "Change Password"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

// ── Header ────────────────────────────────────────────────────────────────────

export default function Header() {
  const [open, setOpen]                             = useState(false)
  const [showChangePassword, setShowChangePassword] = useState(false)
  const ref = useRef<HTMLDivElement>(null)
  const navigate = useNavigate()
  const { user, logout } = useAuthStore()

  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener("mousedown", handleClick)
    return () => document.removeEventListener("mousedown", handleClick)
  }, [])

  async function handleLogout() {
    await authService.logout()
    logout()
    navigate("/login", { replace: true })
  }

  function handleChangePasswordClick() {
    setOpen(false)
    setShowChangePassword(true)
  }

  const initials = user
    ? user.username.slice(0, 2).toUpperCase()
    : "??"

  return (
    <>
      <header className="flex h-16 shrink-0 items-center justify-between border-b bg-white px-6">
        <div />

        <div ref={ref} className="relative">
          <button
            onClick={() => setOpen((p) => !p)}
            className="flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium text-foreground hover:bg-accent transition-colors"
          >
            <span className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-xs font-bold text-white">
              {initials}
            </span>
            <span className="hidden sm:block">{user?.username ?? "User"}</span>
            <ChevronDown className="h-4 w-4 text-muted-foreground" />
          </button>

          {open && (
            <div className="absolute right-0 top-full mt-1 w-52 rounded-md border bg-white py-1 shadow-lg z-50">
              <div className="border-b px-3 py-2">
                <p className="text-sm font-medium">{user?.username}</p>
                <p className="text-xs text-muted-foreground truncate">{user?.email}</p>
              </div>
              <button
                onClick={handleChangePasswordClick}
                className="flex w-full items-center gap-2 px-3 py-2 text-sm text-foreground hover:bg-accent transition-colors"
              >
                <KeyRound className="h-4 w-4" />
                Change Password
              </button>
              <button
                onClick={handleLogout}
                className="flex w-full items-center gap-2 px-3 py-2 text-sm text-foreground hover:bg-accent transition-colors"
              >
                <LogOut className="h-4 w-4" />
                Sign out
              </button>
            </div>
          )}
        </div>
      </header>

      <ChangePasswordDialog
        open={showChangePassword}
        onOpenChange={setShowChangePassword}
      />
    </>
  )
}

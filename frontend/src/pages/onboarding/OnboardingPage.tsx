import React, { useEffect, useRef, useState } from "react"
import toast from "react-hot-toast"
import { ArrowLeft, Plus, CheckCircle2, Upload } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem,
} from "@/components/ui/select"
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
} from "@/components/ui/table"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogClose,
} from "@/components/ui/dialog"
import PageHeader from "@/components/shared/PageHeader"
import EmptyState from "@/components/shared/EmptyState"
import onboardingService, {
  type OnboardingWorkflow,
  type OnboardingTask,
  type OnboardingTemplate,
  type OnboardingDocumentResponse,
} from "@/services/onboardingService"
import employeeService from "@/services/employeeService"
import { toastApiError } from "@/services/api"
import { useAuthStore } from "@/store/authStore"
import { useIsManagerOrAbove } from "@/hooks/useRole"

// ─── Helpers ──────────────────────────────────────────────────────────────────

type WorkflowBadgeVariant = "warning" | "success" | "destructive" | "default"
type TaskBadgeVariant = "secondary" | "warning" | "success" | "destructive" | "default"

function workflowStatusVariant(status: string): WorkflowBadgeVariant {
  switch (status) {
    case "IN_PROGRESS": return "warning"
    case "COMPLETED":   return "success"
    case "CANCELLED":   return "destructive"
    default:            return "default"
  }
}

function taskStatusVariant(status: string): TaskBadgeVariant {
  switch (status) {
    case "PENDING":     return "secondary"
    case "IN_PROGRESS": return "warning"
    case "COMPLETED":   return "success"
    case "SKIPPED":     return "destructive"
    default:            return "default"
  }
}

function formatDate(iso?: string): string {
  if (!iso) return "—"
  return new Date(iso).toLocaleDateString("en-IN", {
    day: "2-digit", month: "short", year: "numeric",
  })
}

// ─── Progress bar ─────────────────────────────────────────────────────────────

function ProgressBar({ completed, total }: { completed: number; total: number }) {
  const pct = total > 0 ? Math.round((completed / total) * 100) : 0
  return (
    <div className="flex items-center gap-2">
      <div className="h-2 w-28 rounded-full bg-gray-200 overflow-hidden">
        <div
          className="h-full rounded-full bg-green-500 transition-all"
          style={{ width: `${pct}%` }}
        />
      </div>
      <span className="text-xs text-muted-foreground whitespace-nowrap">
        {completed}/{total}
      </span>
    </div>
  )
}

// ─── Complete Task dialog ──────────────────────────────────────────────────────

interface CompleteTaskDialogProps {
  task: OnboardingTask | null
  onOpenChange: (open: boolean) => void
  onCompleted: (taskId: string) => void
}

function CompleteTaskDialog({ task, onOpenChange, onCompleted }: CompleteTaskDialogProps) {
  const [remarks, setRemarks] = useState("")
  const [saving, setSaving] = useState(false)

  // Reset remarks when the dialog opens a new task
  useEffect(() => {
    if (task) setRemarks("")
  }, [task?.id])

  function handleConfirm() {
    if (!task) return
    setSaving(true)
    onboardingService
      .completeTask(task.id, remarks.trim() || undefined)
      .then(() => {
        toast.success(`Task "${task.title}" marked as complete.`)
        onCompleted(task.id)
        onOpenChange(false)
      })
      .catch((err) => toastApiError(err, "Failed to complete task."))
      .finally(() => setSaving(false))
  }

  return (
    <Dialog open={!!task} onOpenChange={(open) => !open && onOpenChange(false)}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>Complete Task</DialogTitle>
        </DialogHeader>

        <div className="space-y-3 py-1">
          {task && (
            <p className="text-sm text-muted-foreground">
              Mark <span className="font-medium text-foreground">{task.title}</span> as complete?
            </p>
          )}
          <div className="space-y-1.5">
            <Label>Remarks <span className="text-muted-foreground">(optional)</span></Label>
            <textarea
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-1 focus:ring-ring resize-none"
              rows={3}
              value={remarks}
              onChange={(e) => setRemarks(e.target.value)}
              placeholder="Add any notes…"
            />
          </div>
        </div>

        <DialogFooter>
          <DialogClose asChild>
            <Button variant="outline" disabled={saving}>Cancel</Button>
          </DialogClose>
          <Button onClick={handleConfirm} disabled={saving}>
            {saving ? "Saving…" : "Mark Complete"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

// ─── Upload Document dialog ───────────────────────────────────────────────────

interface UploadDocumentDialogProps {
  task: OnboardingTask | null
  workflowId: string
  onOpenChange: (open: boolean) => void
  onUploaded: (doc: OnboardingDocumentResponse) => void
}

function UploadDocumentDialog({ task, workflowId, onOpenChange, onUploaded }: UploadDocumentDialogProps) {
  const [documentType, setDocumentType] = useState("")
  const [file, setFile] = useState<File | null>(null)
  const [saving, setSaving] = useState(false)
  const fileRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (task) { setDocumentType(""); setFile(null) }
  }, [task?.id])

  function handleSubmit() {
    if (!task || !file || !documentType.trim()) {
      toast.error("Please select a file and enter a document type.")
      return
    }
    setSaving(true)
    onboardingService
      .uploadDocument(workflowId, task.id, documentType.trim(), file)
      .then((res) => {
        const doc: OnboardingDocumentResponse = (res.data as any)?.data ?? res.data
        toast.success("Document uploaded successfully.")
        onUploaded(doc)
        onOpenChange(false)
      })
      .catch((err) => toastApiError(err, "Failed to upload document."))
      .finally(() => setSaving(false))
  }

  return (
    <Dialog open={!!task} onOpenChange={(open) => !open && onOpenChange(false)}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>Upload Document</DialogTitle>
        </DialogHeader>

        <div className="space-y-3 py-1">
          {task && (
            <p className="text-sm text-muted-foreground">
              Uploading for: <span className="font-medium text-foreground">{task.title}</span>
            </p>
          )}
          <div className="space-y-1.5">
            <Label>Document Type</Label>
            <input
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm focus:outline-none focus:ring-1 focus:ring-ring"
              placeholder="e.g. AADHAR, PAN, PHOTO"
              value={documentType}
              onChange={(e) => setDocumentType(e.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <Label>File</Label>
            <input
              ref={fileRef}
              type="file"
              className="hidden"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            />
            <div className="flex items-center gap-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="gap-1"
                onClick={() => fileRef.current?.click()}
              >
                <Upload className="h-4 w-4" />
                {file ? "Change File" : "Choose File"}
              </Button>
              {file && <span className="text-xs text-muted-foreground truncate max-w-[140px]">{file.name}</span>}
            </div>
          </div>
        </div>

        <DialogFooter>
          <DialogClose asChild>
            <Button variant="outline" disabled={saving}>Cancel</Button>
          </DialogClose>
          <Button onClick={handleSubmit} disabled={saving || !file}>
            {saving ? "Uploading…" : "Upload"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

// ─── Tasks detail view ────────────────────────────────────────────────────────

interface TasksViewProps {
  workflow: OnboardingWorkflow
  onBack: () => void
}

function TasksView({ workflow, onBack }: TasksViewProps) {
  const [tasks, setTasks] = useState<OnboardingTask[]>([])
  const [loading, setLoading] = useState(true)
  const [completingTask, setCompletingTask] = useState<OnboardingTask | null>(null)
  const [uploadingTask, setUploadingTask] = useState<OnboardingTask | null>(null)

  useEffect(() => {
    setLoading(true)
    onboardingService
      .getTasks(workflow.id)
      .then((res) => {
        const data = (res.data as any)?.data ?? res.data
        setTasks(Array.isArray(data) ? data : [])
      })
      .catch((err) => toastApiError(err, "Failed to load tasks."))
      .finally(() => setLoading(false))
  }, [workflow.id])

  function handleCompleted(taskId: string) {
    setTasks((prev) =>
      prev.map((t) =>
        t.id === taskId ? { ...t, status: "COMPLETED", completedAt: new Date().toISOString() } : t
      )
    )
  }

  const canComplete = (status: string) => status === "PENDING" || status === "IN_PROGRESS"

  return (
    <div>
      <div className="mb-4">
        <Button variant="ghost" size="sm" onClick={onBack} className="gap-1">
          <ArrowLeft className="h-4 w-4" />
          Back to Onboarding
        </Button>
      </div>

      <PageHeader
        title={`Onboarding — ${workflow.employeeName}`}
        description={`${workflow.employeeCode} · ${workflow.templateName}`}
      />

      {/* Workflow info card */}
      <Card className="mb-6">
        <CardContent className="pt-4">
          <div className="flex flex-wrap gap-x-8 gap-y-2 text-sm">
            <div>
              <span className="text-muted-foreground">Start Date: </span>
              <span className="font-medium">{formatDate(workflow.startDate)}</span>
            </div>
            <div>
              <span className="text-muted-foreground">Status: </span>
              <Badge variant={workflowStatusVariant(workflow.status)}>{workflow.status}</Badge>
            </div>
            <div>
              <span className="text-muted-foreground">Progress: </span>
              <ProgressBar completed={workflow.completedTasks} total={workflow.totalTasks} />
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Tasks list */}
      {loading ? (
        <div className="space-y-3">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-20 w-full" />
          ))}
        </div>
      ) : tasks.length === 0 ? (
        <EmptyState
          title="No tasks found"
          description="This workflow has no tasks yet."
        />
      ) : (
        <div className="space-y-3">
          {tasks.map((task) => (
            <Card key={task.id}>
              <CardContent className="pt-4">
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1 space-y-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <p className="font-medium text-sm">{task.title}</p>
                      <Badge variant={taskStatusVariant(task.status)}>{task.status}</Badge>
                      <span className="text-xs text-muted-foreground bg-muted px-2 py-0.5 rounded">
                        {task.taskType}
                      </span>
                    </div>
                    {task.description && (
                      <p className="text-sm text-muted-foreground">{task.description}</p>
                    )}
                    <div className="flex gap-4 text-xs text-muted-foreground flex-wrap">
                      <span>Due: {formatDate(task.dueDate)}</span>
                      {task.completedAt && (
                        <span>Completed: {formatDate(task.completedAt)}</span>
                      )}
                      {task.remarks && (
                        <span>Remarks: {task.remarks}</span>
                      )}
                    </div>
                  </div>
                  {canComplete(task.status) && (
                    <div className="flex gap-2 shrink-0">
                      {task.taskType === "DOCUMENT_UPLOAD" && (
                        <Button
                          size="sm"
                          variant="outline"
                          className="gap-1"
                          onClick={() => setUploadingTask(task)}
                        >
                          <Upload className="h-4 w-4" />
                          Upload
                        </Button>
                      )}
                      <Button
                        size="sm"
                        variant="outline"
                        className="gap-1"
                        onClick={() => setCompletingTask(task)}
                      >
                        <CheckCircle2 className="h-4 w-4" />
                        Mark Complete
                      </Button>
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <CompleteTaskDialog
        task={completingTask}
        onOpenChange={(open) => !open && setCompletingTask(null)}
        onCompleted={handleCompleted}
      />

      <UploadDocumentDialog
        task={uploadingTask}
        workflowId={workflow.id}
        onOpenChange={(open) => !open && setUploadingTask(null)}
        onUploaded={() => toast.success("Document saved. Mark the task complete when ready.")}
      />
    </div>
  )
}

// ─── Initiate Onboarding dialog ───────────────────────────────────────────────

interface InitiateDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onInitiated: (workflow: OnboardingWorkflow) => void
}

function InitiateDialog({ open, onOpenChange, onInitiated }: InitiateDialogProps) {
  const [employees, setEmployees] = useState<{ id: string; employeeCode: string; fullName: string }[]>([])
  const [templates, setTemplates] = useState<OnboardingTemplate[]>([])
  const [employeeId, setEmployeeId] = useState("")
  const [templateId, setTemplateId] = useState("")
  const [saving, setSaving] = useState(false)
  const [loadingOpts, setLoadingOpts] = useState(false)

  useEffect(() => {
    if (!open) return
    setLoadingOpts(true)
    Promise.all([
      employeeService.getSummaries(),
      onboardingService.getTemplates(),
    ])
      .then(([eRes, tRes]) => {
        const eData = (eRes.data as any)?.data ?? eRes.data
        const tData = (tRes.data as any)?.data ?? tRes.data
        setEmployees(Array.isArray(eData) ? eData : [])
        setTemplates(Array.isArray(tData) ? tData : [])
      })
      .catch((err) => toastApiError(err, "Failed to load options."))
      .finally(() => setLoadingOpts(false))
  }, [open])

  // Reset on close
  useEffect(() => {
    if (!open) {
      setEmployeeId("")
      setTemplateId("")
    }
  }, [open])

  function handleSubmit() {
    if (!employeeId || !templateId) {
      toast.error("Please select both an employee and a template.")
      return
    }
    setSaving(true)
    onboardingService
      .initiateWorkflow(employeeId, templateId)
      .then((res) => {
        const workflow: OnboardingWorkflow = (res.data as any)?.data ?? res.data
        toast.success("Onboarding workflow initiated.")
        onInitiated(workflow)
        onOpenChange(false)
      })
      .catch((err) => toastApiError(err, "Failed to initiate onboarding."))
      .finally(() => setSaving(false))
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>Initiate Onboarding</DialogTitle>
        </DialogHeader>

        <div className="space-y-4 py-2">
          <div className="space-y-1.5">
            <Label>Employee</Label>
            <Select value={employeeId} onValueChange={setEmployeeId} disabled={loadingOpts}>
              <SelectTrigger>
                <SelectValue placeholder={loadingOpts ? "Loading…" : "Select employee"} />
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
            <Label>Template</Label>
            <Select value={templateId} onValueChange={setTemplateId} disabled={loadingOpts}>
              <SelectTrigger>
                <SelectValue placeholder={loadingOpts ? "Loading…" : "Select template"} />
              </SelectTrigger>
              <SelectContent>
                {templates.map((t) => (
                  <SelectItem key={t.id} value={t.id}>
                    {t.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>

        <DialogFooter>
          <DialogClose asChild>
            <Button variant="outline" disabled={saving}>Cancel</Button>
          </DialogClose>
          <Button onClick={handleSubmit} disabled={saving || loadingOpts}>
            {saving ? "Initiating…" : "Initiate"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

// ─── Employee self-view (personal checklist) ──────────────────────────────────

function EmployeeOnboardingView() {
  const user = useAuthStore((s) => s.user)
  const [workflow, setWorkflow] = useState<OnboardingWorkflow | null>(null)
  const [loading, setLoading] = useState(true)
  const [activeWorkflow, setActiveWorkflow] = useState<OnboardingWorkflow | null>(null)

  useEffect(() => {
    if (!user?.id) { setLoading(false); return }
    setLoading(true)
    onboardingService
      .getWorkflowByEmployee(user.id)
      .then((res) => {
        const wf: OnboardingWorkflow = (res.data as any)?.data ?? res.data
        setWorkflow(wf || null)
      })
      .catch(() => setWorkflow(null))
      .finally(() => setLoading(false))
  }, [user?.id])

  if (activeWorkflow) {
    return <TasksView workflow={activeWorkflow} onBack={() => setActiveWorkflow(null)} />
  }

  if (loading) {
    return (
      <div className="space-y-3 p-6">
        {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-10 w-full" />)}
      </div>
    )
  }

  if (!workflow) {
    return (
      <div>
        <PageHeader title="Onboarding" description="Your onboarding checklist." />
        <EmptyState title="No onboarding in progress" description="You don't have an active onboarding workflow." />
      </div>
    )
  }

  return (
    <div>
      <PageHeader title="My Onboarding Checklist" description={`${workflow.templateName} · Step ${workflow.currentStep} of ${workflow.totalSteps}`} />
      <Card>
        <CardContent className="pt-4">
          <div className="flex flex-wrap gap-x-8 gap-y-2 text-sm mb-4">
            <div>
              <span className="text-muted-foreground">Status: </span>
              <Badge variant={workflowStatusVariant(workflow.status)}>{workflow.status}</Badge>
            </div>
            <div>
              <span className="text-muted-foreground">Progress: </span>
              <ProgressBar completed={workflow.completedTasks} total={workflow.totalTasks} />
            </div>
          </div>
          <Button onClick={() => setActiveWorkflow(workflow)}>View Tasks</Button>
        </CardContent>
      </Card>
    </div>
  )
}

// ─── HR Admin / Manager workflow list ─────────────────────────────────────────

function AdminOnboardingView() {
  const [workflows, setWorkflows] = useState<OnboardingWorkflow[]>([])
  const [loading, setLoading] = useState(true)
  const [activeWorkflow, setActiveWorkflow] = useState<OnboardingWorkflow | null>(null)
  const [showInitiateDialog, setShowInitiateDialog] = useState(false)

  function loadWorkflows() {
    setLoading(true)
    onboardingService
      .getWorkflows()
      .then((res) => {
        const data = (res.data as any)?.data ?? res.data
        setWorkflows(Array.isArray(data) ? data : [])
      })
      .catch((err) => toastApiError(err, "Failed to load onboarding workflows."))
      .finally(() => setLoading(false))
  }

  useEffect(() => { loadWorkflows() }, [])

  // ── Tasks detail view ──
  if (activeWorkflow) {
    return (
      <TasksView
        workflow={activeWorkflow}
        onBack={() => setActiveWorkflow(null)}
      />
    )
  }

  // ── Workflows list ──
  return (
    <div>
      <PageHeader
        title="Onboarding"
        description="Track and manage employee onboarding workflows."
        action={
          <Button onClick={() => setShowInitiateDialog(true)} className="gap-1">
            <Plus className="h-4 w-4" />
            Initiate Onboarding
          </Button>
        }
      />

      <Card>
        <CardContent className="p-0">
          {loading ? (
            <div className="space-y-3 p-6">
              {Array.from({ length: 4 }).map((_, i) => (
                <Skeleton key={i} className="h-10 w-full" />
              ))}
            </div>
          ) : workflows.length === 0 ? (
            <EmptyState
              title="No onboarding workflows"
              description="Initiate an onboarding workflow to get started."
              action={
                <Button onClick={() => setShowInitiateDialog(true)} className="gap-1">
                  <Plus className="h-4 w-4" />
                  Initiate Onboarding
                </Button>
              }
            />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Employee</TableHead>
                  <TableHead>Joining Template</TableHead>
                  <TableHead>Start Date</TableHead>
                  <TableHead>Days Active</TableHead>
                  <TableHead>Progress</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {workflows.map((wf) => (
                  <TableRow
                    key={wf.id}
                    className="cursor-pointer hover:bg-muted/50"
                    onClick={() => setActiveWorkflow(wf)}
                  >
                    <TableCell>
                      <div>
                        <p className="font-medium text-sm">{wf.employeeName}</p>
                        <p className="text-xs text-muted-foreground">{wf.employeeCode}</p>
                      </div>
                    </TableCell>
                    <TableCell>{wf.templateName}</TableCell>
                    <TableCell>{formatDate(wf.startDate)}</TableCell>
                    <TableCell>
                      {wf.daysInProgress != null
                        ? <span className="text-sm">{wf.daysInProgress}d</span>
                        : <span className="text-muted-foreground">—</span>}
                    </TableCell>
                    <TableCell onClick={(e) => e.stopPropagation()}>
                      <ProgressBar completed={wf.completedTasks} total={wf.totalTasks} />
                    </TableCell>
                    <TableCell onClick={(e) => e.stopPropagation()}>
                      <Badge variant={workflowStatusVariant(wf.status)}>{wf.status}</Badge>
                    </TableCell>
                    <TableCell onClick={(e) => e.stopPropagation()}>
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => setActiveWorkflow(wf)}
                      >
                        View Tasks
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <InitiateDialog
        open={showInitiateDialog}
        onOpenChange={setShowInitiateDialog}
        onInitiated={(wf) => setWorkflows((prev) => [wf, ...prev])}
      />
    </div>
  )
}

// ─── Main OnboardingPage — routes by role ─────────────────────────────────────

export default function OnboardingPage() {
  const isManagerOrAbove = useIsManagerOrAbove()
  return isManagerOrAbove ? <AdminOnboardingView /> : <EmployeeOnboardingView />
}

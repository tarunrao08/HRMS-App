import React, { useEffect, useState } from "react"
import { Users, Clock, Calendar, IndianRupee, CheckCircle2, Circle, ArrowRight, FileDown } from "lucide-react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { useNavigate } from "react-router-dom"
import api from "@/services/api"
import payrollService from "@/services/payrollService"
import type { Payslip } from "@/services/payrollService"
import onboardingService, { type OnboardingWorkflow, type OnboardingTask } from "@/services/onboardingService"
import { useAuthStore } from "@/store/authStore"
import { useIsHrAdmin } from "@/hooks/useRole"
import AttendanceCard from "./AttendanceCard"

interface StatValue {
  loading: boolean
  value: string
}

const MONTH_NAMES = [
  "January","February","March","April","May","June",
  "July","August","September","October","November","December",
]

function formatIndianCurrency(amount: number): string {
  const formatted = new Intl.NumberFormat("en-IN", {
    maximumFractionDigits: 0,
  }).format(amount)
  return `₹${formatted}`
}

function getTodayString(): string {
  const d = new Date()
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, "0")
  const day = String(d.getDate()).padStart(2, "0")
  return `${year}-${month}-${day}`
}

// ─── Onboarding Checklist Card ────────────────────────────────────────────────

function taskStatusIcon(status: string) {
  if (status === "COMPLETED")
    return <CheckCircle2 className="h-4 w-4 text-green-500 shrink-0" />
  if (status === "IN_PROGRESS")
    return <ArrowRight className="h-4 w-4 text-amber-500 shrink-0" />
  return <Circle className="h-4 w-4 text-muted-foreground shrink-0" />
}

interface OnboardingCardProps {
  workflow: OnboardingWorkflow
  tasks: OnboardingTask[]
}

function OnboardingCard({ workflow, tasks }: OnboardingCardProps) {
  const navigate = useNavigate()
  const pct =
    workflow.totalTasks > 0
      ? Math.round((workflow.completedTasks / workflow.totalTasks) * 100)
      : 0

  return (
    <Card className="border-amber-200 bg-amber-50/40">
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <CardTitle className="text-base text-amber-900">Onboarding Checklist</CardTitle>
          <Badge variant="warning" className="text-xs">
            {workflow.status.replace("_", " ")}
          </Badge>
        </div>
        <div className="flex items-center gap-2 mt-1">
          <div className="h-2 flex-1 rounded-full bg-amber-100 overflow-hidden">
            <div
              className="h-full rounded-full bg-amber-500 transition-all"
              style={{ width: `${pct}%` }}
            />
          </div>
          <span className="text-xs text-amber-700 whitespace-nowrap font-medium">
            {workflow.completedTasks}/{workflow.totalTasks} done
          </span>
        </div>
      </CardHeader>
      <CardContent>
        {tasks.length === 0 ? (
          <p className="text-sm text-muted-foreground">Loading tasks…</p>
        ) : (
          <ul className="space-y-2">
            {tasks.map((task) => (
              <li key={task.id} className="flex items-center gap-2 text-sm">
                {taskStatusIcon(task.status)}
                <span
                  className={
                    task.status === "COMPLETED" ? "line-through text-muted-foreground" : ""
                  }
                >
                  {task.title}
                </span>
              </li>
            ))}
          </ul>
        )}
        <Button
          variant="outline"
          size="sm"
          className="mt-4 w-full border-amber-300 text-amber-800 hover:bg-amber-100"
          onClick={() => navigate("/onboarding")}
        >
          View Full Checklist
        </Button>
      </CardContent>
    </Card>
  )
}

// ─── Main DashboardPage ───────────────────────────────────────────────────────

export default function DashboardPage() {
  const user = useAuthStore((s) => s.user)
  const isHrAdmin = useIsHrAdmin()

  const [totalEmployees, setTotalEmployees] = useState<StatValue>({ loading: true, value: "—" })
  const [presentToday, setPresentToday]     = useState<StatValue>({ loading: true, value: "—" })
  const [pendingLeaves, setPendingLeaves]   = useState<StatValue>({ loading: true, value: "—" })
  const [monthPayroll, setMonthPayroll]     = useState<StatValue>({ loading: true, value: "—" })

  const [myPayslip, setMyPayslip]           = useState<Payslip | null>(null)
  const [payslipLoading, setPayslipLoading] = useState(false)

  const [onboardingWorkflow, setOnboardingWorkflow] = useState<OnboardingWorkflow | null>(null)
  const [onboardingTasks, setOnboardingTasks]       = useState<OnboardingTask[]>([])
  const [onboardingLoading, setOnboardingLoading]   = useState(false)

  useEffect(() => {
    const today = getTodayString()

    const p1 = api.get("/employees", { params: { size: 1 } })
    const p2 = api.get("/attendance", { params: { date: today, status: "PRESENT", size: 1 } })
    const p3 = isHrAdmin
      ? api.get("/leave/requests", { params: { status: "PENDING", size: 1 } })
      : api.get("/leave/requests/me", { params: { status: "PENDING", size: 1 } })
    // HR admin: sum of all active employees' monthly salary; Employee: their own monthly salary
    const p4 = isHrAdmin
      ? api.get("/payroll/salary-structures/summary")
      : api.get("/payroll/salary-structures/my")

    Promise.allSettled([p1, p2, p3, p4]).then(([r1, r2, r3, r4]) => {
      if (r1.status === "fulfilled") {
        const total = r1.value.data?.totalElements ?? r1.value.data?.data?.totalElements ?? "—"
        setTotalEmployees({ loading: false, value: String(total) })
      } else {
        setTotalEmployees({ loading: false, value: "—" })
      }

      if (r2.status === "fulfilled") {
        const total = r2.value.data?.totalElements ?? r2.value.data?.data?.totalElements ?? "—"
        setPresentToday({ loading: false, value: String(total) })
      } else {
        setPresentToday({ loading: false, value: "—" })
      }

      if (r3.status === "fulfilled") {
        const total = r3.value.data?.totalElements ?? r3.value.data?.data?.totalElements ?? "—"
        setPendingLeaves({ loading: false, value: String(total) })
      } else {
        setPendingLeaves({ loading: false, value: "—" })
      }

      if (r4.status === "fulfilled") {
        const d = r4.value.data?.data ?? r4.value.data
        if (isHrAdmin) {
          const amount = d?.totalMonthlyPayable ?? 0
          setMonthPayroll({ loading: false, value: amount > 0 ? formatIndianCurrency(amount) : "—" })
        } else {
          const amount = d?.monthlyGross ?? d?.grossSalary ?? 0
          setMonthPayroll({ loading: false, value: amount > 0 ? formatIndianCurrency(amount) : "—" })
        }
      } else {
        setMonthPayroll({ loading: false, value: "—" })
      }
    })
  }, [isHrAdmin])

  useEffect(() => {
    if (isHrAdmin) return
    setPayslipLoading(true)
    payrollService.getMyLatestPayslip()
      .then((res) => {
        const data = (res.data as any)?.data ?? res.data
        setMyPayslip(data ?? null)
      })
      .catch(() => setMyPayslip(null))
      .finally(() => setPayslipLoading(false))
  }, [isHrAdmin])

  async function downloadPayslipPdf(id: string, month: number, year: number) {
    try {
      const res = await payrollService.downloadPayslipPdf(id)
      const url = URL.createObjectURL(new Blob([res.data as BlobPart], { type: "application/pdf" }))
      const a = document.createElement("a")
      a.href = url
      a.download = `payslip_${year}_${String(month).padStart(2, "0")}.pdf`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
    } catch {
      // silently fail — PDF not yet generated
    }
  }

  useEffect(() => {
    if (!user?.id) return
    setOnboardingLoading(true)
    onboardingService
      .getWorkflowByEmployee(user.id)
      .then((res) => {
        const wf: OnboardingWorkflow = (res.data as any)?.data ?? res.data
        if (!wf || wf.status === "COMPLETED") return null
        setOnboardingWorkflow(wf)
        return onboardingService.getTasks(wf.id)
      })
      .then((tasksRes) => {
        if (!tasksRes) return
        const tasks = (tasksRes.data as any)?.data ?? tasksRes.data
        setOnboardingTasks(Array.isArray(tasks) ? tasks : [])
      })
      .catch(() => setOnboardingWorkflow(null))
      .finally(() => setOnboardingLoading(false))
  }, [user?.id])

  const stats = [
    { title: "Total Employees",                                                stat: totalEmployees, icon: Users,       color: "text-blue-600",   bg: "bg-blue-50" },
    { title: "Present Today",                                                  stat: presentToday,   icon: Clock,       color: "text-green-600",  bg: "bg-green-50" },
    { title: "Pending Leaves",                                                 stat: pendingLeaves,  icon: Calendar,    color: "text-amber-600",  bg: "bg-amber-50" },
    { title: isHrAdmin ? "Total Monthly Payroll" : "My Monthly Salary",       stat: monthPayroll,   icon: IndianRupee, color: "text-purple-600", bg: "bg-purple-50" },
  ]

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Dashboard</h1>
        <p className="text-muted-foreground">Welcome back! Here's what's happening today.</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map((s) => (
          <Card key={s.title}>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                {s.title}
              </CardTitle>
              <div className={`rounded-md p-2 ${s.bg}`}>
                <s.icon className={`h-4 w-4 ${s.color}`} />
              </div>
            </CardHeader>
            <CardContent>
              {s.stat.loading ? (
                <Skeleton className="h-8 w-24" />
              ) : (
                <p className="text-2xl font-bold">{s.stat.value}</p>
              )}
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Employee payslip summary card */}
      {!isHrAdmin && (
        <div className="grid gap-4 lg:grid-cols-2">
          {payslipLoading ? (
            <Skeleton className="h-48 w-full rounded-xl" />
          ) : myPayslip ? (
            <Card className="border-purple-200 bg-purple-50/30">
              <CardHeader className="pb-3">
                <div className="flex items-center justify-between">
                  <CardTitle className="text-base text-purple-900">My Latest Payslip</CardTitle>
                  <Badge variant="secondary">
                    {MONTH_NAMES[myPayslip.month - 1]} {myPayslip.year}
                  </Badge>
                </div>
              </CardHeader>
              <CardContent>
                <div className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Gross Pay</span>
                    <span className="font-medium">{formatIndianCurrency(myPayslip.grossSalary)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">TDS</span>
                    <span className="text-destructive">−{formatIndianCurrency(myPayslip.tds)}</span>
                  </div>
                  <div className="flex justify-between border-t pt-2 font-semibold">
                    <span>Net Pay</span>
                    <span className="text-purple-700">{formatIndianCurrency(myPayslip.netSalary)}</span>
                  </div>
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  className="mt-3 w-full border-purple-300 text-purple-800 hover:bg-purple-100"
                  onClick={() => downloadPayslipPdf(myPayslip.id, myPayslip.month, myPayslip.year)}
                >
                  <FileDown className="mr-2 h-4 w-4" />
                  Download PDF
                </Button>
              </CardContent>
            </Card>
          ) : (
            <Card>
              <CardContent className="py-8 text-center">
                <p className="text-sm text-muted-foreground">No published payslip available yet.</p>
              </CardContent>
            </Card>
          )}
        </div>
      )}

      <div className="grid gap-4 lg:grid-cols-2">
        {!isHrAdmin && user?.id ? (
          <AttendanceCard employeeId={user.id} />
        ) : (
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Recent Attendance</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground">Attendance data will appear here.</p>
            </CardContent>
          </Card>
        )}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">
              {isHrAdmin ? "Pending Leave Requests" : "My Pending Leave Requests"}
            </CardTitle>
          </CardHeader>
          <CardContent>
            {pendingLeaves.loading ? (
              <Skeleton className="h-8 w-16" />
            ) : (
              <div className="flex items-baseline gap-2">
                <p className="text-3xl font-bold">
                  {pendingLeaves.value === "—" ? "0" : pendingLeaves.value}
                </p>
                <p className="text-sm text-muted-foreground">
                  {isHrAdmin ? "requests awaiting approval" : "of your requests awaiting approval"}
                </p>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Onboarding checklist for employees with an active workflow */}
      {(onboardingLoading || onboardingWorkflow) && (
        <div className="grid gap-4 lg:grid-cols-2">
          {onboardingLoading ? (
            <Skeleton className="h-64 w-full rounded-xl" />
          ) : onboardingWorkflow ? (
            <OnboardingCard workflow={onboardingWorkflow} tasks={onboardingTasks} />
          ) : null}
        </div>
      )}
    </div>
  )
}

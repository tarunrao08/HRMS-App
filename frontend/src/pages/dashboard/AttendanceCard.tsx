import React, { useEffect, useState } from "react"
import { ChevronLeft, ChevronRight } from "lucide-react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import api from "@/services/api"

// ── Types ─────────────────────────────────────────────────────────────────────

interface Summary {
  presentDays: number
  absentDays: number
  lateDays: number
  halfDays: number
  totalWorkingHours: number | string
  workingDays: number
}

interface DailyRecord {
  id: string
  attendanceDate: string
  status: string
  workingHours: number | null
  punchIn: string | null
  punchOut: string | null
}

// ── Helpers ───────────────────────────────────────────────────────────────────

const MONTH_NAMES = [
  "January","February","March","April","May","June",
  "July","August","September","October","November","December",
]

const STATUS_STYLE: Record<string, string> = {
  PRESENT:        "bg-green-100 text-green-700",
  ABSENT:         "bg-red-100 text-red-700",
  LATE:           "bg-amber-100 text-amber-700",
  HALF_DAY:       "bg-blue-100 text-blue-700",
  ON_LEAVE:       "bg-purple-100 text-purple-700",
  HOLIDAY:        "bg-gray-100 text-gray-600",
  WEEKEND:        "bg-gray-50 text-gray-400",
  WORK_FROM_HOME: "bg-teal-100 text-teal-700",
  REGULARIZED:    "bg-indigo-100 text-indigo-700",
}

function lastDayOf(year: number, month: number) {
  return new Date(year, month, 0).getDate()
}

function pad(n: number) {
  return String(n).padStart(2, "0")
}

// ── Component ─────────────────────────────────────────────────────────────────

export default function AttendanceCard({ employeeId }: { employeeId: string }) {
  const now = new Date()

  const [year, setYear]       = useState(now.getFullYear())
  const [month, setMonth]     = useState(now.getMonth() + 1)
  const [summary, setSummary] = useState<Summary | null>(null)
  const [records, setRecords] = useState<DailyRecord[]>([])
  const [loading, setLoading] = useState(false)
  const [noData, setNoData]   = useState(false)

  useEffect(() => {
    if (!employeeId) return

    setLoading(true)
    setSummary(null)
    setRecords([])
    setNoData(false)

    const from = `${year}-${pad(month)}-01`
    const to   = `${year}-${pad(month)}-${pad(lastDayOf(year, month))}`

    Promise.allSettled([
      api.get(`/attendance/employee/${employeeId}/monthly-summary`, { params: { year, month } }),
      api.get(`/attendance/employee/${employeeId}`, { params: { from, to, size: 31 } }),
    ]).then(([summaryRes, recordsRes]) => {
      let gotSummary = false
      let gotRecords = false

      if (summaryRes.status === "fulfilled") {
        const data = (summaryRes.value.data as any)?.data ?? summaryRes.value.data
        if (data) { setSummary(data); gotSummary = true }
      }

      if (recordsRes.status === "fulfilled") {
        const data = (recordsRes.value.data as any)?.data ?? recordsRes.value.data
        const list: DailyRecord[] = data?.content ?? (Array.isArray(data) ? data : [])
        if (list.length > 0) { setRecords(list); gotRecords = true }
      }

      setNoData(!gotSummary && !gotRecords)
    }).finally(() => setLoading(false))
  }, [employeeId, year, month])

  // ── Navigation ───────────────────────────────────────────────────────────

  function prevMonth() {
    if (month === 1) { setYear(y => y - 1); setMonth(12) }
    else setMonth(m => m - 1)
  }

  function nextMonth() {
    const isCurrentMonth = year === now.getFullYear() && month === now.getMonth() + 1
    if (isCurrentMonth) return
    if (month === 12) { setYear(y => y + 1); setMonth(1) }
    else setMonth(m => m + 1)
  }

  function jumpToMonth(value: string) {
    if (!value) return
    const [y, m] = value.split("-").map(Number)
    setYear(y); setMonth(m)
  }

  const isCurrentMonth = year === now.getFullYear() && month === now.getMonth() + 1
  const maxMonth = `${now.getFullYear()}-${pad(now.getMonth() + 1)}`

  // ── Render ────────────────────────────────────────────────────────────────

  return (
    <Card>
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between gap-2 flex-wrap">
          <CardTitle className="text-base">My Attendance</CardTitle>

          <div className="flex items-center gap-1">
            {/* Prev */}
            <Button variant="ghost" size="sm" className="h-7 w-7 p-0" onClick={prevMonth}>
              <ChevronLeft className="h-4 w-4" />
            </Button>

            {/* Month label */}
            <span className="text-sm font-medium w-32 text-center">
              {MONTH_NAMES[month - 1]} {year}
            </span>

            {/* Next (disabled on current month) */}
            <Button
              variant="ghost"
              size="sm"
              className="h-7 w-7 p-0"
              onClick={nextMonth}
              disabled={isCurrentMonth}
            >
              <ChevronRight className="h-4 w-4" />
            </Button>

            {/* Month picker */}
            <input
              type="month"
              value={`${year}-${pad(month)}`}
              max={maxMonth}
              onChange={(e) => jumpToMonth(e.target.value)}
              className="border rounded px-1.5 py-0.5 text-xs text-muted-foreground cursor-pointer"
            />

            {/* Jump to current */}
            {!isCurrentMonth && (
              <Button
                variant="outline"
                size="sm"
                className="h-7 text-xs px-2"
                onClick={() => { setYear(now.getFullYear()); setMonth(now.getMonth() + 1) }}
              >
                Current
              </Button>
            )}
          </div>
        </div>
      </CardHeader>

      <CardContent>
        {loading ? (
          <div className="space-y-2">
            <Skeleton className="h-8 w-full" />
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-3/4" />
          </div>
        ) : noData ? (
          <p className="text-sm text-muted-foreground text-center py-8">
            No attendance records found for {MONTH_NAMES[month - 1]} {year}.
          </p>
        ) : (
          <>
            {/* Monthly summary chips */}
            {summary && (
              <div className="flex flex-wrap gap-1.5 mb-3">
                <span className="text-xs px-2 py-0.5 rounded-full bg-green-100 text-green-700 font-medium">
                  Present: {summary.presentDays}
                </span>
                <span className="text-xs px-2 py-0.5 rounded-full bg-red-100 text-red-700 font-medium">
                  Absent: {summary.absentDays}
                </span>
                {summary.lateDays > 0 && (
                  <span className="text-xs px-2 py-0.5 rounded-full bg-amber-100 text-amber-700 font-medium">
                    Late: {summary.lateDays}
                  </span>
                )}
                {summary.halfDays > 0 && (
                  <span className="text-xs px-2 py-0.5 rounded-full bg-blue-100 text-blue-700 font-medium">
                    Half Day: {summary.halfDays}
                  </span>
                )}
                <span className="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-600 font-medium">
                  Hours: {Number(summary.totalWorkingHours ?? 0).toFixed(1)}h
                </span>
              </div>
            )}

            {/* Daily records list */}
            {records.length === 0 ? (
              <p className="text-sm text-muted-foreground text-center py-6">
                No daily records found for {MONTH_NAMES[month - 1]} {year}.
              </p>
            ) : (
              <div className="space-y-0 max-h-56 overflow-y-auto pr-1">
                {records.map((r) => {
                  const d = new Date(r.attendanceDate + "T00:00:00")
                  const dayName  = d.toLocaleDateString("en-IN", { weekday: "short" })
                  const dateStr  = d.toLocaleDateString("en-IN", { day: "2-digit", month: "short" })
                  const statusLabel = r.status.replace("_", " ")
                  const styleClass  = STATUS_STYLE[r.status] ?? "bg-gray-100 text-gray-600"
                  const hours = r.workingHours != null ? `${Number(r.workingHours).toFixed(1)}h` : "—"

                  return (
                    <div
                      key={r.id}
                      className="flex items-center justify-between py-1.5 text-sm border-b last:border-0"
                    >
                      <span className="text-muted-foreground w-28 shrink-0">
                        {dayName}, {dateStr}
                      </span>
                      <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${styleClass}`}>
                        {statusLabel}
                      </span>
                      <span className="text-xs text-muted-foreground w-10 text-right shrink-0">
                        {hours}
                      </span>
                    </div>
                  )
                })}
              </div>
            )}
          </>
        )}
      </CardContent>
    </Card>
  )
}

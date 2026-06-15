import { NavLink } from "react-router-dom"
import {
  LayoutDashboard,
  Users,
  Building2,
  Award,
  MapPin,
  Clock,
  CalendarDays,
  CalendarCheck,
  CalendarRange,
  DollarSign,
  ClipboardList,
  KeyRound,
} from "lucide-react"
import { cn } from "@/lib/utils"
import { Separator } from "@/components/ui/separator"
import { useAuthStore } from "@/store/authStore"

interface NavItemDef {
  icon: React.ElementType
  label: string
  to: string
  roles?: string[]
}

const mainNav: NavItemDef[] = [
  { icon: LayoutDashboard, label: "Dashboard", to: "/dashboard" },
]

const workforceNav: NavItemDef[] = [
  { icon: Users,     label: "Employees",     to: "/employees",    roles: ["ROLE_HR_ADMIN", "ROLE_MANAGER"] },
  { icon: KeyRound,  label: "User Accounts", to: "/users",        roles: ["ROLE_HR_ADMIN"] },
  { icon: Building2, label: "Departments",   to: "/departments",  roles: ["ROLE_HR_ADMIN"] },
  { icon: Award,     label: "Designations",  to: "/designations", roles: ["ROLE_HR_ADMIN"] },
  { icon: MapPin,    label: "Branches",      to: "/branches",     roles: ["ROLE_HR_ADMIN"] },
]

const hrNav: NavItemDef[] = [
  { icon: Clock,         label: "Attendance",   to: "/attendance" },
  { icon: CalendarDays,  label: "My Leave",     to: "/leave" },
  { icon: CalendarCheck, label: "Approvals",    to: "/leave/approvals", roles: ["ROLE_HR_ADMIN", "ROLE_MANAGER"] },
  { icon: CalendarRange, label: "Leave Admin",  to: "/leave/admin",     roles: ["ROLE_HR_ADMIN"] },
  { icon: DollarSign,    label: "Payroll",      to: "/payroll",    roles: ["ROLE_HR_ADMIN"] },
  { icon: ClipboardList, label: "Onboarding",   to: "/onboarding", roles: ["ROLE_HR_ADMIN", "ROLE_MANAGER", "ROLE_EMPLOYEE"] },
]

function useVisibleItems(items: NavItemDef[], userRoles: string[]): NavItemDef[] {
  return items.filter((item) => {
    if (!item.roles) return true
    return item.roles.some((r) => userRoles.includes(r))
  })
}

function NavItem({ icon: Icon, label, to }: NavItemDef) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        cn(
          "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors",
          isActive
            ? "bg-sidebar-active text-sidebar-active-foreground"
            : "text-sidebar-foreground/70 hover:bg-white/10 hover:text-sidebar-foreground"
        )
      }
    >
      <Icon className="h-4 w-4 shrink-0" />
      {label}
    </NavLink>
  )
}

function NavSection({ title, items }: { title: string; items: NavItemDef[] }) {
  if (items.length === 0) return null
  return (
    <div className="space-y-1">
      <p className="px-3 pb-1 pt-3 text-xs font-semibold uppercase tracking-wider text-sidebar-foreground/40">
        {title}
      </p>
      {items.map((item) => (
        <NavItem key={item.to} {...item} />
      ))}
    </div>
  )
}

export default function Sidebar() {
  const user = useAuthStore((s) => s.user)
  const roles = user?.roles ?? []

  const visibleWorkforce = useVisibleItems(workforceNav, roles)
  const visibleHr        = useVisibleItems(hrNav, roles)

  return (
    <aside className="flex h-screen w-60 shrink-0 flex-col bg-sidebar">
      {/* Logo */}
      <div className="flex h-16 items-center gap-2 px-5">
        <div className="flex h-8 w-8 items-center justify-center rounded-md bg-primary text-white font-bold text-sm">
          HR
        </div>
        <span className="text-lg font-semibold text-sidebar-foreground">HRMS</span>
      </div>

      <Separator className="bg-sidebar-border" />

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto px-3 py-2">
        <div className="space-y-1">
          {mainNav.map((item) => (
            <NavItem key={item.to} {...item} />
          ))}
        </div>
        <NavSection title="Workforce" items={visibleWorkforce} />
        <NavSection title="HR Modules" items={visibleHr} />
      </nav>

      <Separator className="bg-sidebar-border" />

      {/* Footer */}
      <div className="px-4 py-3 text-xs text-sidebar-foreground/40">
        © 2026 HRMS
      </div>
    </aside>
  )
}

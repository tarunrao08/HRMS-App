import { useAuthStore } from "@/store/authStore"

export function useHasRole(...roles: string[]): boolean {
  const user = useAuthStore((s) => s.user)
  if (!user) return false
  return roles.some((r) => user.roles.includes(r))
}

export function useIsHrAdmin(): boolean {
  return useHasRole("ROLE_HR_ADMIN")
}

export function useIsManagerOrAbove(): boolean {
  return useHasRole("ROLE_HR_ADMIN", "ROLE_MANAGER")
}

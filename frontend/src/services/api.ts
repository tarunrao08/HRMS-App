import axios, { AxiosError, InternalAxiosRequestConfig } from "axios"
import toast from "react-hot-toast"

// ── Axios instance ────────────────────────────────────────────────────────────

const api = axios.create({
  baseURL: "/api",
  headers: { "Content-Type": "application/json" },
  timeout: 30_000,
})

// ── Helpers ───────────────────────────────────────────────────────────────────

/** Extract the most useful human-readable message from an Axios error. */
export function extractErrorMessage(error: unknown, fallback = "An unexpected error occurred."): string {
  if (!axios.isAxiosError(error)) return fallback

  const data = (error as AxiosError<{ message?: string; data?: Record<string, string> }>).response?.data

  // Validation error: data.data is a {field: message} map — show the first one
  if (data?.data && typeof data.data === "object" && !Array.isArray(data.data)) {
    const first = Object.values(data.data as Record<string, string>)[0]
    if (first) return first
  }

  // Standard ApiResponse message
  if (data?.message) return data.message

  // Network / timeout
  if (!error.response) return "Network error — check your connection and try again."

  return fallback
}

function readStoredTokens(): { accessToken: string | null; refreshToken: string | null } {
  try {
    const raw = localStorage.getItem("hrms-auth")
    if (!raw) return { accessToken: null, refreshToken: null }
    const { state } = JSON.parse(raw)
    return { accessToken: state?.accessToken ?? null, refreshToken: state?.refreshToken ?? null }
  } catch {
    return { accessToken: null, refreshToken: null }
  }
}

function updateStoredTokens(accessToken: string, refreshToken: string) {
  try {
    const raw = localStorage.getItem("hrms-auth")
    if (!raw) return
    const parsed = JSON.parse(raw)
    parsed.state.accessToken  = accessToken
    parsed.state.refreshToken = refreshToken
    localStorage.setItem("hrms-auth", JSON.stringify(parsed))
  } catch {
    // ignore
  }
}

function forceLogout() {
  localStorage.removeItem("hrms-auth")
  window.location.href = "/login"
}

// ── Request interceptor — attach JWT ─────────────────────────────────────────

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const { accessToken } = readStoredTokens()
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

// ── Response interceptor ──────────────────────────────────────────────────────

let isRefreshing = false
let failedQueue: Array<{ resolve: (token: string) => void; reject: (err: unknown) => void }> = []

function flushQueue(error: unknown, token: string | null) {
  failedQueue.forEach((p) => (error ? p.reject(error) : p.resolve(token!)))
  failedQueue = []
}

type RetryableConfig = InternalAxiosRequestConfig & { _retry?: boolean }

api.interceptors.response.use(
  (res) => res,
  async (error: AxiosError) => {
    const original = error.config as RetryableConfig | undefined
    const status   = error.response?.status

    // ── 401: attempt silent token refresh ──────────────────────────────────
    if (status === 401 && original && !original._retry) {
      if (isRefreshing) {
        // Queue this request until the refresh completes
        return new Promise<string>((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then((token) => {
          original.headers.Authorization = `Bearer ${token}`
          return api(original)
        })
      }

      original._retry = true
      isRefreshing    = true

      const { refreshToken } = readStoredTokens()
      if (!refreshToken) {
        isRefreshing = false
        flushQueue(error, null)
        forceLogout()
        return Promise.reject(error)
      }

      try {
        const res = await axios.post<{ data: { accessToken: string; refreshToken: string } }>(
          "/api/auth/refresh",
          { refreshToken }
        )
        const { accessToken: newAccess, refreshToken: newRefresh } = res.data.data
        updateStoredTokens(newAccess, newRefresh)
        flushQueue(null, newAccess)
        original.headers.Authorization = `Bearer ${newAccess}`
        return api(original)
      } catch (refreshErr) {
        flushQueue(refreshErr, null)
        forceLogout()
        return Promise.reject(refreshErr)
      } finally {
        isRefreshing = false
      }
    }

    // ── 403: access denied ─────────────────────────────────────────────────
    if (status === 403) {
      toast.error("Access denied — you don't have permission to perform this action.", {
        id: "access-denied",   // deduplicate: same id = only one toast shown at a time
      })
      return Promise.reject(error)
    }

    // ── 5xx / network errors ───────────────────────────────────────────────
    if (!error.response) {
      toast.error("Network error — check your connection and try again.", {
        id: "network-error",
      })
      return Promise.reject(error)
    }

    if (status !== undefined && status >= 500) {
      toast.error("Server error — please try again in a moment.", {
        id: "server-error",
      })
      return Promise.reject(error)
    }

    // All other errors (400, 404, 409, 422, …) — let the calling code handle them
    return Promise.reject(error)
  }
)

/**
 * Show a toast for an API call failure.
 * Silently skips 403, 5xx, and network errors because the response interceptor
 * already toasted those — avoids duplicate messages.
 * For everything else (400, 404, 409, 422…) it shows the backend's own message
 * if available, otherwise falls back to `fallbackMessage`.
 */
export function toastApiError(error: unknown, fallbackMessage: string): void {
  if (axios.isAxiosError(error)) {
    const status = error.response?.status
    // Already handled by the interceptor — don't double-toast
    if (!status || status === 403 || status >= 500) return
  }
  toast.error(extractErrorMessage(error, fallbackMessage))
}

export default api

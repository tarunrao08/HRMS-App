/**
 * Role-Based Access Control (RBAC)
 *
 * Tests verify that:
 *   - HR Admin can access all routes
 *   - The /403 Forbidden page renders correctly
 *   - Protected routes redirect unauthenticated users to /login
 *
 * Testing EMPLOYEE role access denial requires an employee-role user.
 * Those tests are marked with setup instructions and use test.skip
 * until an employee user is created via the admin UI.
 *
 * To create an employee user:
 *   1. Login as admin
 *   2. Create an employee record (Employees page)
 *   3. The system will generate credentials — update EMPLOYEE_CREDS below
 */

import { test, expect } from "@playwright/test"
import { loginAsAdmin, clearAuthStorage } from "./helpers/auth"

// Update these once an employee-role user is created
const EMPLOYEE_CREDS = { username: "", password: "" }

const HR_ADMIN_ROUTES = [
  "/dashboard",
  "/employees",
  "/attendance",
  "/leave",
  "/payroll",
  "/onboarding",
  "/departments",
  "/designations",
  "/branches",
]

const EMPLOYEE_BLOCKED_ROUTES = [
  "/departments",
  "/designations",
  "/branches",
  "/employees",
  "/payroll",
  "/onboarding",
]

test.describe("Unauthenticated access", () => {
  test.beforeEach(async ({ page }) => {
    await clearAuthStorage(page)
  })

  for (const route of ["/dashboard", "/employees", "/departments", "/leave", "/payroll"]) {
    test(`redirects ${route} to /login when not authenticated`, async ({ page }) => {
      await page.goto(route)
      await expect(page).toHaveURL(/\/login/, { timeout: 5_000 })
    })
  }
})

test.describe("HR Admin access", () => {
  test.beforeEach(async ({ page }) => {
    await clearAuthStorage(page)
    await loginAsAdmin(page)
  })

  for (const route of HR_ADMIN_ROUTES) {
    test(`HR Admin can access ${route}`, async ({ page }) => {
      await page.goto(route)
      // Should not be redirected to /login or /403
      await expect(page).not.toHaveURL(/\/login/, { timeout: 5_000 })
      await expect(page).not.toHaveURL(/\/403/, { timeout: 5_000 })
    })
  }

  test("HR Admin sidebar shows all nav items", async ({ page }) => {
    await page.goto("/dashboard")
    const nav = page.locator("nav")
    await expect(nav.getByRole("link", { name: "Employees" })).toBeVisible()
    await expect(nav.getByRole("link", { name: "Departments" })).toBeVisible()
    await expect(nav.getByRole("link", { name: "Designations" })).toBeVisible()
    await expect(nav.getByRole("link", { name: "Branches" })).toBeVisible()
    await expect(nav.getByRole("link", { name: "Payroll" })).toBeVisible()
    await expect(nav.getByRole("link", { name: "Onboarding" })).toBeVisible()
  })
})

test.describe("403 Forbidden page", () => {
  test("renders correctly when navigating directly to /403", async ({ page }) => {
    await clearAuthStorage(page)
    await loginAsAdmin(page)
    await page.goto("/403")
    await expect(page.getByRole("heading", { name: "Access Denied" })).toBeVisible()
    await expect(page.getByRole("button", { name: "Go to Dashboard" })).toBeVisible()
    await expect(page.getByRole("button", { name: "Go Back" })).toBeVisible()
  })

  test("Go to Dashboard button navigates to /dashboard", async ({ page }) => {
    await clearAuthStorage(page)
    await loginAsAdmin(page)
    await page.goto("/403")
    await page.getByRole("button", { name: "Go to Dashboard" }).click()
    await expect(page).toHaveURL(/\/dashboard/)
  })
})

test.describe("Employee role access denial", () => {
  test.skip(!EMPLOYEE_CREDS.username, "Set EMPLOYEE_CREDS username/password to enable these tests")

  test.beforeEach(async ({ page }) => {
    await clearAuthStorage(page)
    await page.goto("/login")
    await page.getByLabel("Username").fill(EMPLOYEE_CREDS.username)
    await page.getByLabel("Password").fill(EMPLOYEE_CREDS.password)
    await page.getByRole("button", { name: "Sign In" }).click()
    await page.waitForURL("**/dashboard")
  })

  for (const route of EMPLOYEE_BLOCKED_ROUTES) {
    test(`Employee is redirected to /403 when accessing ${route}`, async ({ page }) => {
      await page.goto(route)
      await expect(page).toHaveURL(/\/403/, { timeout: 5_000 })
      await expect(page.getByRole("heading", { name: "Access Denied" })).toBeVisible()
    })
  }

  test("Employee sidebar does NOT show Departments, Payroll, Employees", async ({ page }) => {
    await page.goto("/dashboard")
    const nav = page.locator("nav")
    await expect(nav.getByRole("link", { name: "Departments" })).not.toBeVisible()
    await expect(nav.getByRole("link", { name: "Payroll" })).not.toBeVisible()
    await expect(nav.getByRole("link", { name: "Employees" })).not.toBeVisible()
  })

  test("Employee can access /attendance and /leave", async ({ page }) => {
    for (const route of ["/attendance", "/leave"]) {
      await page.goto(route)
      await expect(page).not.toHaveURL(/\/403/)
      await expect(page).not.toHaveURL(/\/login/)
    }
  })

  test("Employee Leave page does NOT show All Requests tab", async ({ page }) => {
    await page.goto("/leave")
    await expect(page.getByRole("button", { name: "All Requests" })).not.toBeVisible()
  })
})

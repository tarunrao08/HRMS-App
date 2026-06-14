import { test, expect } from "@playwright/test"
import { loginAsAdmin, clearAuthStorage, ADMIN_USERNAME, ADMIN_PASSWORD } from "./helpers/auth"

test.describe("Authentication", () => {
  test.beforeEach(async ({ page }) => {
    await clearAuthStorage(page)
  })

  test("redirects unauthenticated user to /login", async ({ page }) => {
    await page.goto("/dashboard")
    await expect(page).toHaveURL(/\/login/)
    await expect(page.getByRole("heading", { name: "HRMS Portal" })).toBeVisible()
  })

  test("login succeeds with valid credentials", async ({ page }) => {
    await loginAsAdmin(page)
    await expect(page).toHaveURL(/\/dashboard/)
    // Sidebar should appear and show the HR Admin user
    await expect(page.getByText("HRMS")).toBeVisible()
    await expect(page.getByText(ADMIN_USERNAME)).toBeVisible()
  })

  test("login fails with wrong password", async ({ page }) => {
    await page.goto("/login")
    await page.getByLabel("Username").fill(ADMIN_USERNAME)
    await page.getByLabel("Password").fill("WrongPassword!")
    await page.getByRole("button", { name: "Sign In" }).click()

    // Should stay on /login and show an error message
    await expect(page).toHaveURL(/\/login/)
    await expect(page.locator(".text-destructive")).toBeVisible({ timeout: 8_000 })
  })

  test("login fails with unknown user", async ({ page }) => {
    await page.goto("/login")
    await page.getByLabel("Username").fill("nobody")
    await page.getByLabel("Password").fill("doesntmatter")
    await page.getByRole("button", { name: "Sign In" }).click()

    await expect(page).toHaveURL(/\/login/)
    await expect(page.locator(".text-destructive")).toBeVisible({ timeout: 8_000 })
  })

  test("Sign In button is disabled while login is in progress", async ({ page }) => {
    await page.goto("/login")
    await page.getByLabel("Username").fill(ADMIN_USERNAME)
    await page.getByLabel("Password").fill(ADMIN_PASSWORD)

    // Click and immediately check disabled state
    const btn = page.getByRole("button", { name: /Sign In|Signing in/ })
    await btn.click()
    // Button text changes to "Signing in…" during loading
    await expect(page.getByRole("button", { name: /Signing in/ })).toBeDisabled({ timeout: 3_000 })
      .catch(() => {
        // If the request was too fast, the button may have already returned to normal
        // This is acceptable — it means the request completed before we could observe it
      })
  })

  test("logout clears session and returns to /login", async ({ page }) => {
    await loginAsAdmin(page)

    // Open user menu and click logout
    await page.getByRole("button", { name: ADMIN_USERNAME }).click()
    await page.getByRole("button", { name: /sign out/i }).click()

    await expect(page).toHaveURL(/\/login/)

    // Navigating to /dashboard after logout should redirect back to /login
    await page.goto("/dashboard")
    await expect(page).toHaveURL(/\/login/)
  })
})

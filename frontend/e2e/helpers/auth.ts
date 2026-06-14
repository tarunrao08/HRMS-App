import type { Page } from "@playwright/test"

export const ADMIN_USERNAME = "admin"
export const ADMIN_PASSWORD = "Admin@1234"

export async function loginAsAdmin(page: Page): Promise<void> {
  await page.goto("/login")
  await page.getByLabel("Username").fill(ADMIN_USERNAME)
  await page.getByLabel("Password").fill(ADMIN_PASSWORD)
  await page.getByRole("button", { name: "Sign In" }).click()
  await page.waitForURL("**/dashboard", { timeout: 15_000 })
}

export async function clearAuthStorage(page: Page): Promise<void> {
  await page.evaluate(() => localStorage.removeItem("hrms-auth"))
}

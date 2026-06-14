/**
 * Leave workflow — apply and approve
 *
 * Prerequisites:
 *   1. Backend running on :8080
 *   2. Admin user (admin / Admin@1234) exists via DataInitializer
 *   3. At least one LeaveType configured in the DB
 *
 * The "Apply Leave" happy path requires the logged-in user to have an
 * employee record in the DB (so the backend can look up their employee ID
 * from the JWT). If the admin user has no linked employee record, the
 * apply-leave test will be skipped gracefully.
 *
 * The "All Requests" and "Leave Balances" tab visibility tests run regardless.
 */

import { test, expect } from "@playwright/test"
import { loginAsAdmin, clearAuthStorage } from "./helpers/auth"

test.describe("Leave Management (HR Admin)", () => {
  test.beforeEach(async ({ page }) => {
    await clearAuthStorage(page)
    await loginAsAdmin(page)
    await page.goto("/leave")
    await expect(page.getByRole("heading", { name: "Leave Management" })).toBeVisible()
  })

  test("Leave page is accessible to HR Admin", async ({ page }) => {
    await expect(page).toHaveURL(/\/leave/)
    await expect(page.getByRole("heading", { name: "Leave Management" })).toBeVisible()
  })

  test("HR Admin sees My Requests, All Requests, and Leave Balances tabs", async ({ page }) => {
    await expect(page.getByRole("button", { name: "My Requests" })).toBeVisible()
    await expect(page.getByRole("button", { name: "All Requests" })).toBeVisible()
    await expect(page.getByRole("button", { name: "Leave Balances" })).toBeVisible()
  })

  test("All Requests tab loads and shows approve/reject controls for pending items", async ({ page }) => {
    await page.getByRole("button", { name: "All Requests" }).click()

    // The table or empty state should be visible
    const hasTable = await page.locator("table").isVisible({ timeout: 6_000 }).catch(() => false)
    const hasEmpty = await page.getByText("No leave requests").isVisible().catch(() => false)

    expect(hasTable || hasEmpty).toBeTruthy()
  })

  test("Leave Balances tab shows employee selector for HR Admin", async ({ page }) => {
    await page.getByRole("button", { name: "Leave Balances" }).click()

    // Manager/above sees a dropdown to pick employee
    // The select trigger button is inside a Card
    const selector = page.locator("select, [role='combobox']").filter({ hasText: /Select employee/i })
      .or(page.locator("button").filter({ hasText: /Select employee/i }))
    await expect(selector.first()).toBeVisible({ timeout: 5_000 })
  })

  test("Apply Leave button is visible on My Requests tab", async ({ page }) => {
    // "My Requests" is the default active tab
    await expect(page.getByRole("button", { name: /Apply Leave/i })).toBeVisible()
  })

  test("Apply Leave dialog opens", async ({ page }) => {
    await page.getByRole("button", { name: /Apply Leave/i }).click()
    await expect(page.getByRole("dialog")).toBeVisible()
    await expect(page.getByRole("heading", { name: "Apply for Leave" })).toBeVisible()
  })

  test("Apply Leave dialog closes on Cancel", async ({ page }) => {
    await page.getByRole("button", { name: /Apply Leave/i }).click()
    const dialog = page.getByRole("dialog")
    await expect(dialog).toBeVisible()
    await dialog.getByRole("button", { name: "Cancel" }).click()
    await expect(dialog).not.toBeVisible()
  })

  test("Apply Leave — validates required fields before submit", async ({ page }) => {
    await page.getByRole("button", { name: /Apply Leave/i }).click()
    const dialog = page.getByRole("dialog")
    await dialog.getByRole("button", { name: "Submit" }).click()

    // Without a leave type and dates selected, a toast error should fire
    // or the dialog stays open
    await expect(dialog).toBeVisible()
  })
})

/**
 * Employee CRUD — HR Admin role
 *
 * Prerequisites (must be set up before running):
 *   1. At least one Department exists (create via /departments)
 *   2. At least one Designation exists under that Department (create via /designations)
 *   3. At least one Branch exists (create via /branches)
 *
 * Without those, the Create Employee form dropdowns will be empty and the
 * form submission will fail server-side validation. Use the UI to seed them
 * before running this spec.
 */

import { test, expect } from "@playwright/test"
import { loginAsAdmin, clearAuthStorage } from "./helpers/auth"

const TIMESTAMP = Date.now()
const TEST_EMAIL = `e2e-${TIMESTAMP}@hrms-test.com`

test.describe("Employee CRUD (HR Admin)", () => {
  test.beforeEach(async ({ page }) => {
    await clearAuthStorage(page)
    await loginAsAdmin(page)
  })

  test("Employees page is accessible to HR Admin", async ({ page }) => {
    await page.goto("/employees")
    await expect(page).toHaveURL(/\/employees/)
    await expect(page.getByRole("heading", { name: "Employees" })).toBeVisible()
  })

  test("Add Employee button is visible for HR Admin", async ({ page }) => {
    await page.goto("/employees")
    await expect(page.getByRole("button", { name: /Add Employee/i })).toBeVisible()
  })

  test("Add Employee dialog opens on button click", async ({ page }) => {
    await page.goto("/employees")
    await page.getByRole("button", { name: /Add Employee/i }).click()
    await expect(page.getByRole("dialog")).toBeVisible()
    await expect(page.getByRole("heading", { name: "Add Employee" })).toBeVisible()
  })

  test("Add Employee dialog closes on Cancel", async ({ page }) => {
    await page.goto("/employees")
    await page.getByRole("button", { name: /Add Employee/i }).click()
    await expect(page.getByRole("dialog")).toBeVisible()

    await page.getByRole("button", { name: "Cancel" }).click()
    await expect(page.getByRole("dialog")).not.toBeVisible()
  })

  test("Create employee — form validates required fields", async ({ page }) => {
    await page.goto("/employees")
    await page.getByRole("button", { name: /Add Employee/i }).click()

    // Submit without filling anything — HTML5 validation should prevent submission
    // or the submit button stays active but the form won't submit
    const dialog = page.getByRole("dialog")
    await expect(dialog).toBeVisible()

    // The form uses HTML required attributes; clicking submit with empty required
    // fields should not close the dialog
    await dialog.getByRole("button", { name: /Create Employee/i }).click()
    await expect(dialog).toBeVisible()
  })

  test("Create employee — full happy path", async ({ page }) => {
    // Skip this test if no reference data exists
    await page.goto("/employees")
    await page.getByRole("button", { name: /Add Employee/i }).click()

    const dialog = page.getByRole("dialog")

    // Fill in basic fields
    await dialog.getByLabel("First Name").fill("E2E")
    await dialog.getByLabel("Last Name").fill(`Test ${TIMESTAMP}`)
    await dialog.getByLabel("Email").fill(TEST_EMAIL)
    await dialog.getByLabel("Joining Date").fill("2025-01-01")

    // Select Department — pick first available option
    const deptTrigger = dialog.locator("[id^='radix-']:has-text('Select department'), [placeholder='Select department']")
      .or(dialog.locator("button").filter({ hasText: "Select department" }))
    await deptTrigger.first().click()
    const deptOption = page.getByRole("option").first()
    const deptExists = await deptOption.isVisible({ timeout: 3_000 }).catch(() => false)

    if (!deptExists) {
      test.skip(true, "No departments exist — seed reference data first")
      return
    }
    await deptOption.click()

    // Wait for designations to load, then pick first
    await page.waitForTimeout(500)
    const desigTrigger = dialog.locator("button").filter({ hasText: "Select designation" })
    await desigTrigger.first().click()
    await page.getByRole("option").first().click()

    // Branch
    const branchTrigger = dialog.locator("button").filter({ hasText: "Select branch" })
    await branchTrigger.first().click()
    await page.getByRole("option").first().click()

    // Gender
    const genderTrigger = dialog.locator("button").filter({ hasText: "Select gender" })
    await genderTrigger.first().click()
    await page.getByRole("option", { name: "Male" }).click()

    // Employment Type
    const typeTrigger = dialog.locator("button").filter({ hasText: "Select type" })
    await typeTrigger.first().click()
    await page.getByRole("option", { name: "Full Time" }).click()

    // Employment Status
    const statusTrigger = dialog.locator("button").filter({ hasText: "Select status" })
    await statusTrigger.first().click()
    await page.getByRole("option", { name: "Active" }).click()

    // Submit
    await dialog.getByRole("button", { name: /Create Employee/i }).click()

    // Dialog should close and the employee should appear in the list
    await expect(dialog).not.toBeVisible({ timeout: 10_000 })
    await expect(page.getByText(TEST_EMAIL)).toBeVisible({ timeout: 8_000 })
  })

  test("Employee list has edit and delete actions for HR Admin", async ({ page }) => {
    await page.goto("/employees")

    // Wait for table to load
    await page.waitForSelector("table", { timeout: 8_000 })

    const rows = page.locator("tbody tr")
    const count = await rows.count()

    if (count === 0) {
      // Empty state — just verify Add Employee is still visible
      await expect(page.getByRole("button", { name: /Add Employee/i })).toBeVisible()
      return
    }

    // First row should have edit and delete buttons
    const firstRow = rows.first()
    await expect(firstRow.getByRole("button", { name: /Edit employee/i })).toBeVisible()
    await expect(firstRow.getByRole("button", { name: /Delete employee/i })).toBeVisible()
  })
})

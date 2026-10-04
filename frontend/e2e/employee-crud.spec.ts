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
 *
 * The Add/Edit Employee dialog is a 4-step wizard — Basic Info / Documents &
 * Bank / Family / Nominees — navigated via "Save and Next" (validates the
 * current tab before advancing) and "Previous" (free, no validation). Only
 * the last tab (Nominees) has the real submit button ("Create Employee" /
 * "Update Employee"). Tabs ahead of the furthest one reached are disabled;
 * tabs already passed are always clickable to jump back.
 */

import { test, expect, type Page, type Locator } from "@playwright/test"
import { loginAsAdmin, clearAuthStorage } from "./helpers/auth"

const TIMESTAMP = Date.now()
const TEST_EMAIL = `e2e-${TIMESTAMP}@hrms-test.com`

/** Selects the first available option from a Radix Select triggered by its placeholder text. */
async function selectFirstOption(page: Page, dialog: Locator, placeholder: string) {
  await dialog.locator("button").filter({ hasText: placeholder }).first().click()
  await page.getByRole("option").first().click()
}

/** Selects a specific option by exact visible text from a Radix Select. */
async function selectOptionByName(page: Page, dialog: Locator, placeholder: string, optionName: string) {
  await dialog.locator("button").filter({ hasText: placeholder }).first().click()
  await page.getByRole("option", { name: optionName, exact: true }).last().click()
}

/** Fills every required Basic Info field and clicks "Save and Next". Returns false and skips the test if no reference data (departments) exists. */
async function fillBasicInfoAndAdvance(page: Page, dialog: Locator, opts: {
  firstName: string; lastName: string; email: string
}): Promise<boolean> {
  await dialog.getByLabel("First Name").fill(opts.firstName)
  await dialog.getByLabel("Last Name").fill(opts.lastName)
  await dialog.getByLabel("Email").fill(opts.email)
  await dialog.locator("#phone").fill("9876543210")
  await dialog.locator("#dateOfBirth").fill("1995-05-15")
  await dialog.getByLabel("Joining Date").fill("2025-01-01")

  await dialog.locator("button").filter({ hasText: "Select department" }).first().click()
  const deptOption = page.getByRole("option").first()
  const deptExists = await deptOption.isVisible({ timeout: 3_000 }).catch(() => false)
  if (!deptExists) return false
  await deptOption.click()

  await page.waitForTimeout(500)
  await selectFirstOption(page, dialog, "Select designation")
  await selectFirstOption(page, dialog, "Select branch")
  await selectOptionByName(page, dialog, "Select gender", "Male")
  await selectOptionByName(page, dialog, "Select type", "Full Time")
  await selectOptionByName(page, dialog, "Select status", "Active")

  await dialog.getByRole("button", { name: "Save and Next" }).click()
  return true
}

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

  test("Add Employee dialog opens on the Basic Info tab with Save and Next", async ({ page }) => {
    await page.goto("/employees")
    await page.getByRole("button", { name: /Add Employee/i }).click()
    const dialog = page.getByRole("dialog")
    await expect(dialog).toBeVisible()
    await expect(page.getByRole("heading", { name: "Add Employee" })).toBeVisible()

    await expect(dialog.getByRole("tab", { name: "Basic Info", selected: true })).toBeVisible()
    await expect(dialog.getByRole("button", { name: "Save and Next" })).toBeVisible()
    // First tab: no Previous, no submit button yet
    await expect(dialog.getByRole("button", { name: "Previous" })).not.toBeVisible()
    await expect(dialog.getByRole("button", { name: "Create Employee" })).not.toBeVisible()
  })

  test("Add Employee dialog closes on Cancel when the form is untouched", async ({ page }) => {
    await page.goto("/employees")
    await page.getByRole("button", { name: /Add Employee/i }).click()
    await expect(page.getByRole("dialog")).toBeVisible()

    await page.getByRole("button", { name: "Cancel" }).click()
    await expect(page.getByRole("dialog")).not.toBeVisible()
  })

  test("Cancel on a dirty form prompts to discard changes", async ({ page }) => {
    await page.goto("/employees")
    await page.getByRole("button", { name: /Add Employee/i }).click()
    const dialog = page.getByRole("dialog")
    await dialog.getByLabel("First Name").fill("Dirty")

    await dialog.getByRole("button", { name: "Cancel" }).click()

    // Dialog should stay open, with a discard-confirmation prompt
    await expect(dialog).toBeVisible()
    await expect(page.getByRole("heading", { name: "Discard changes?" })).toBeVisible()

    // "Keep Editing" returns to the form without losing data
    await page.getByRole("button", { name: "Keep Editing" }).click()
    await expect(page.getByRole("heading", { name: "Discard changes?" })).not.toBeVisible()
    await expect(dialog.getByLabel("First Name")).toHaveValue("Dirty")

    // Cancel again, this time confirm discarding
    await dialog.getByRole("button", { name: "Cancel" }).click()
    await page.getByRole("button", { name: "Discard" }).click()
    await expect(dialog).not.toBeVisible()
  })

  test("X close button also prompts to discard changes on a dirty form", async ({ page }) => {
    await page.goto("/employees")
    await page.getByRole("button", { name: /Add Employee/i }).click()
    const dialog = page.getByRole("dialog")
    await dialog.getByLabel("Last Name").fill("Dirty")

    await dialog.getByRole("button", { name: "Close" }).click()

    await expect(dialog).toBeVisible()
    await expect(page.getByRole("heading", { name: "Discard changes?" })).toBeVisible()
    await page.getByRole("button", { name: "Discard" }).click()
    await expect(dialog).not.toBeVisible()
  })

  test("Documents & Bank / Family / Nominees tabs are disabled until reached", async ({ page }) => {
    await page.goto("/employees")
    await page.getByRole("button", { name: /Add Employee/i }).click()
    const dialog = page.getByRole("dialog")
    await expect(dialog).toBeVisible()

    await expect(dialog.getByRole("tab", { name: "Documents & Bank" })).toBeDisabled()
    await expect(dialog.getByRole("tab", { name: "Family" })).toBeDisabled()
    await expect(dialog.getByRole("tab", { name: "Nominees" })).toBeDisabled()
  })

  test("Save and Next blocks advancing when required Basic Info fields are empty", async ({ page }) => {
    await page.goto("/employees")
    await page.getByRole("button", { name: /Add Employee/i }).click()
    const dialog = page.getByRole("dialog")
    await expect(dialog).toBeVisible()

    await dialog.getByRole("button", { name: "Save and Next" }).click()

    // Should stay on Basic Info — Documents & Bank tab still not reachable
    await expect(dialog.getByRole("tab", { name: "Basic Info", selected: true })).toBeVisible()
    await expect(dialog.getByRole("tab", { name: "Documents & Bank" })).toBeDisabled()
    await expect(page.getByText("First name is required").first()).toBeVisible()
  })

  test("Create employee — full happy path through all four tabs", async ({ page }) => {
    await page.goto("/employees")
    await page.getByRole("button", { name: /Add Employee/i }).click()
    const dialog = page.getByRole("dialog")

    const advanced = await fillBasicInfoAndAdvance(page, dialog, {
      firstName: "E2E", lastName: `Test ${TIMESTAMP}`, email: TEST_EMAIL,
    })
    if (!advanced) {
      test.skip(true, "No departments exist — seed reference data first")
      return
    }

    // Now on Documents & Bank — Previous should be visible, leave everything optional/blank
    await expect(dialog.getByRole("tab", { name: "Documents & Bank", selected: true })).toBeVisible()
    await expect(dialog.getByRole("button", { name: "Previous" })).toBeVisible()
    await dialog.getByRole("button", { name: "Save and Next" }).click()

    // Now on Family — leave empty, it's optional
    await expect(dialog.getByRole("tab", { name: "Family", selected: true })).toBeVisible()
    await dialog.getByRole("button", { name: "Save and Next" }).click()

    // Now on Nominees — the real submit button appears here
    await expect(dialog.getByRole("tab", { name: "Nominees", selected: true })).toBeVisible()
    await expect(dialog.getByRole("button", { name: "Save", exact: true })).toBeVisible()
    await expect(dialog.getByRole("button", { name: "Create Employee" })).toBeVisible()

    await dialog.getByRole("button", { name: "Create Employee" }).click()

    // Dialog should close and the employee should appear in the list
    // (the table shows name, not email, so assert on the unique last name)
    await expect(dialog).not.toBeVisible({ timeout: 10_000 })
    await expect(page.getByText(`Test ${TIMESTAMP}`)).toBeVisible({ timeout: 8_000 })
  })

  test("Previous navigates back without losing entered data", async ({ page }) => {
    await page.goto("/employees")
    await page.getByRole("button", { name: /Add Employee/i }).click()
    const dialog = page.getByRole("dialog")

    const advanced = await fillBasicInfoAndAdvance(page, dialog, {
      firstName: "Nav", lastName: "Back", email: `nav-back-${Date.now()}@hrms-test.com`,
    })
    if (!advanced) {
      test.skip(true, "No departments exist — seed reference data first")
      return
    }

    await expect(dialog.getByRole("tab", { name: "Documents & Bank", selected: true })).toBeVisible()
    await dialog.getByRole("button", { name: "Previous" }).click()

    await expect(dialog.getByRole("tab", { name: "Basic Info", selected: true })).toBeVisible()
    await expect(dialog.getByLabel("First Name")).toHaveValue("Nav")
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

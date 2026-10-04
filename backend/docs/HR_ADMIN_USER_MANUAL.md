# HRMS — HR Admin User Manual

**Audience:** HR Admin (`ROLE_HR_ADMIN`) — this role already combines full administrative rights (create/edit/delete, payroll approval, system configuration) with day-to-day HR operations. There is no separate "Admin" role in this system; HR Admin is the top role.

**Scope:** Employee management, org structure setup (Branches/Departments/Designations), Attendance, Leave, Payroll, Onboarding, User Accounts, and the built-in HRMS Assistant chatbot.

---

## Table of Contents

1. [Getting Started](#1-getting-started)
2. [First-Time Setup — Do This in Order](#2-first-time-setup--do-this-in-order)
3. [Employee Management](#3-employee-management)
4. [User Accounts (Logins)](#4-user-accounts-logins)
5. [Attendance](#5-attendance)
6. [Leave Management](#6-leave-management)
7. [Payroll](#7-payroll)
8. [Onboarding](#8-onboarding)
9. [HRMS Assistant (Chatbot)](#9-hrms-assistant-chatbot)
10. [Roles & Who Can See What](#10-roles--who-can-see-what)
11. [Tips & Troubleshooting](#11-tips--troubleshooting)

---

## 1. Getting Started

### 1.1 Logging In

1. Open the HRMS portal in your browser.
2. Enter your **Username** and **Password**.
3. Click **Sign In**.

If your credentials are rejected, double-check with whoever created your account — an HR Admin creates every login (see [Section 4](#4-user-accounts-logins)), there is no self-registration or "forgot password" flow yet.

### 1.2 The Layout

- **Left sidebar** — your navigation. Menu items are grouped into:
  - **Workforce**: Employees, User Accounts, Departments, Designations, Branches
  - **HR Modules**: Attendance, My Leave, Approvals, Leave Admin, Leave Balances, Leave Types, Payroll, Onboarding
  - Items you don't have permission for simply don't appear — as HR Admin you see everything.
- **Dashboard** — your landing page after login.
- **Chat bubble** (bottom-right, every page) — the HRMS Assistant. See [Section 9](#9-hrms-assistant-chatbot).

---

## 2. First-Time Setup — Do This in Order

The org-structure screens depend on each other, so set them up in this exact sequence the first time you use the system (or whenever opening a new office). Doing it out of order just means empty dropdowns later.

```
1. Branches         →  2. Departments  →  3. Assign Departments to Branch
        ↓                                          ↓
4. Designations (per department)         5. Employees (branch → dept → designation)
        ↓
6. User Accounts (login for each employee, optional but needed for self-service)
```

### Step 1 — Create Departments
**Departments → Add Department**
- **Name*** (e.g. "Engineering")
- **Description** (optional)

Departments are org-wide (not branch-specific by themselves) — you link them to branches in the next step.

### Step 2 — Create Branches and Attach Departments
**Branches → Add Branch**
- **Name***, **City***, **State***, **Country*** — required
- **Address** — optional
- **Departments offered by this branch** — tick every department that operates out of this location.

> ⚠️ **Important:** Only departments checked here will be selectable when you later add an employee at this branch. If you forget a department, edit the branch and check it before creating employees there.

### Step 3 — Create Designations
**Designations → Add Designation**
- **Name*** (e.g. "Senior Engineer")
- **Department*** — pick from existing departments
- **Description** — optional
- **Managerial role** checkbox — tick this if people holding this designation should be selectable as a **Reporting Manager** for others. Only managerial designations show up in the "Reporting Manager" dropdown on the employee form.

Designation names must be unique per department (you can reuse "Manager" in two different departments, but not twice inside one).

### Step 4 — Now You're Ready for Employees
Continue to [Section 3](#3-employee-management).

---

## 3. Employee Management

**Employees** page — add, edit, search, filter, and deactivate employee records.

### 3.1 Filtering / Finding Employees
Use the search box (name, employee code, or email) plus the **Branch**, **Department**, and **Status** dropdowns above the table. The department dropdown narrows to whatever the selected branch offers.

### 3.2 Adding an Employee
Click **Add Employee**. The form is a 4-tab wizard — you must complete each tab in order (later tabs stay locked until you pass the current one, and reopening after an error jumps you straight back to the offending tab):

#### Tab 1 — Basic Info
| Field | Notes |
|---|---|
| First / Last Name* | required |
| Email* | must be a valid email format |
| Phone* | 10-digit Indian mobile (starts 6–9); a leading `+91`/`91`/`0` is stripped automatically as you type |
| Date of Birth* | employee must be **at least 21 years old** — age is auto-calculated and shown next to the field |
| Branch* | pick first — this scopes the Department list below |
| Department* | scoped to the branch's assigned departments (Step 2 above) |
| Designation* | scoped to the selected department |
| Reporting Manager | optional — only employees in the same department with a **managerial** designation appear here |
| Shift | optional — pick from the pre-configured active shifts; shift definitions themselves aren't managed from this screen |
| Joining Date* | |
| Gender* | Male / Female / Other |
| Employment Type* | Full Time / Part Time / Contract / Intern |
| Employment Status* | Active / Inactive — choosing Inactive reveals a **Resignation Date** field |
| Address, City, State, Pincode | optional; Pincode must be a valid 6-digit Indian PIN if filled |
| Profile Picture URL | optional |

#### Tab 2 — Documents & Bank
All optional, but validated if filled:
- **PAN**: format `ABCDE1234F`
- **Aadhar**: exactly 12 digits
- **Bank Account Number**, **Bank Name**
- **IFSC Code**: format `ABCD0123456`
- **Emergency Contact** name / phone / relation — phone validated as a 10-digit mobile number if entered

#### Tab 3 — Family
Click **+ Add Family Member** for each dependent. Per row: Name*, Relationship* (Father/Mother/Spouse/Son/Daughter/Brother/Sister/Other), Date of Birth (must be in the past), Gender, Occupation, Contact Number. A completely blank row is silently ignored, so you don't need to delete unused rows.

#### Tab 4 — Nominees
Click **+ Add Nominee** for each nominee (e.g. for gratuity/PF/insurance). Per row: Name*, Relationship*, Date of Birth* (required — used to auto-detect minors), Share %* (0–100, nominees don't need to add up to exactly 100 across all rows), Address, Contact Number. If the computed age is under 18, the row automatically flags **Minor** and requires a **Guardian Name** and **Guardian Relationship**.

Click **Create** on the last tab to save.

### 3.3 Editing an Employee
Click the pencil icon on any row — the same 4-tab form opens pre-filled, including their currently assigned shift.

### 3.4 "Deleting" an Employee
The trash icon is labeled Delete in the UI, but it does **not** erase the record — it **deactivates** the employee (sets status to Inactive). Historical attendance, leave, and payslip data is preserved. To reverse it, edit the employee and set Employment Status back to Active.

---

## 4. User Accounts (Logins)

**User Accounts** page — this is separate from the employee record. Creating an employee does **not** automatically give them a login; you must create one here if they need to log into the portal themselves.

**Create Account**:
1. **Employee*** — search/select from employees who don't already have an account.
2. **Username*** — auto-suggested from the employee's name (e.g. "john.doe"); editable.
3. **Initial Password*** — set something the employee can use to log in the first time (min. 6 characters). Share it with them directly — there's no automated welcome email yet.
4. **Role*** — Employee / Manager / HR Admin. This determines what the person can see once logged in (see [Section 10](#10-roles--who-can-see-what)).

The accounts table shows username, linked employee, role badges, enabled/disabled status, and last login.

---

## 5. Attendance

**Attendance** page — shows **today's** attendance for everyone, refreshed on demand (Refresh button).

- Filter by name/email/code, Department, or Shift.
- Each row shows the employee's current status: **Present**, **Absent**, **Half Day**, **On Leave**, or **Not Marked**.
- As HR Admin you can click **Present** or **Absent** to mark/correct someone's attendance for today.
- Rows already locked by an approved leave (**On Leave** / **Half Day**) show a 🔒 **Locked** indicator instead of buttons — leave-driven attendance isn't manually overridden here; manage it through the Leave module instead.

This page is day-of view only; historical attendance is consumed internally by payroll (LOP/paid-days calculation) rather than browsed here.

---

## 6. Leave Management

Five related screens, all under the **HR Modules** section of the sidebar.

### 6.1 Leave Types — configure the master list first
**Leave Types** page. Before employees can apply for any leave, at least one Leave Type must exist.

**Add Leave Type** fields:
- **Name*, Code*** (e.g. "Casual Leave", "CL")
- **Max Days/Year** — the default annual allocation
- **Min Notice Days** — how many days in advance it must be applied
- **Applicable Gender** — restrict to Male/Female/Other only, or leave as "Everyone"
- **Paid leave** checkbox
- **Requires supporting document** checkbox
- **Carry-forward allowed** — if checked, set **Max Carry-Forward Days**
- **Encashment allowed** checkbox
- **Active** — uncheck to stop it being offered for new balance allocation without deleting history
- **Tenure-based allocation** — check this if days-per-year should scale with years of service instead of a flat number

**Tenure Tiers** (only after saving a tenure-based leave type once — edit it again to add tiers):
- Each tier is a **Min Years – Max Years → Days** rule (e.g. 0–1 yr → 12 days, 1–3 yrs → 15, 3–5 yrs → 18, 5+ yrs [leave Max blank for unbounded] → 21).
- Ranges can't overlap with each other.
- If no tier matches an employee, allocation falls back to the plain **Max Days/Year** value.

### 6.2 My Leave — applying for your own leave
Every user, including HR Admin, has a personal **My Leave** page: balance cards per leave type (Allocated / Used / Pending / Available) and a request history table. Click a balance card or **Apply Leave** to submit a request (leave type, from/to date or half-day + half, reason). Pending requests can be **Cancelled** from here.

### 6.3 Approvals — acting on requests routed to you
**Approvals** page lists leave requests currently awaiting your sign-off (as a reporting manager or HR Admin, depending on the approval level shown as L1/L2). **Approve** with one click, or **Reject** with a mandatory reason.

### 6.4 Leave Admin — see everything, any employee, any status
**Leave Admin** page is the HR-wide view: filter all leave requests across the company by employee and/or status (Pending/Approved/Rejected/Cancelled). Read-only overview — act on pending ones from the Approvals page instead.

### 6.5 Leave Balances — view and manually adjust
**Leave Balances** page: pick an employee, see their Allocated/Used/Pending/Available days per leave type for the current year.
- **Adjust** — add or deduct days manually (e.g. a comp-off grant, or a correction). Enter a signed number of days (positive to add, negative to deduct) and a mandatory reason — every adjustment is logged.
- **History** (clock icon) — view the full audit trail of adjustments for that leave type, with who made each change and why.

---

## 7. Payroll

**Payroll** page — two building blocks feed payroll: an employee's **Salary Structure (CTC)**, and a monthly **Payroll Run**.

### 7.1 Set an Employee's CTC (do this before running payroll)
Click **Set CTC**:
1. Pick the **Employee**.
2. Choose how you're entering the number — **Annual CTC**, **Monthly Gross**, or **Monthly Basic** — and type the amount. The other two figures are computed live.
3. Set **Effective From** date.
4. Review/adjust the **Salary Components** table — each earning component (Basic, HRA, DA, Conveyance, etc., as configured in the payroll component catalog) can be **Fixed (₹)**, a **Percentage** (of gross or of another component, e.g. HRA as % of Basic), or **Formula** (exactly one component may auto-absorb whatever's left over so the total always reconciles to gross). Percentage fields for standard components are constrained to sensible ranges (e.g. Basic 40–50% of gross) — the field turns red if you go outside that band.
5. The live preview below shows Monthly Gross, the component breakdown, and estimated statutory deductions (PF, ESI, Professional Tax) and net salary before TDS.
6. Click **Save CTC**.

Re-opening **Set CTC** for an employee who already has a structure pre-fills their current values so you can revise it (a new effective-dated version is created).

### 7.2 Running Monthly Payroll
Click **New Run**, pick **Month** and **Year**, then **Create Run**. A run starts in **DRAFT**.

Lifecycle (buttons appear only when the action is valid for the run's current state):
```
DRAFT  --[Process]-->  PROCESSED  --[Approve]-->  APPROVED  -->  DISBURSED
```
- **Process** — calculates gross/deductions/net for every employee based on their salary structure and actual attendance (LOP days).
- **Approve** — locks the numbers in and generates payslip PDFs for everyone in the run.
- View totals (employees, gross, deductions, net) directly in the runs table.

### 7.3 Viewing / Downloading Payslips
Click **View Payslips** on any run to see every employee's payslip for that period. Click a row (or **Details**) to open the breakdown: working/LOP/paid days, earnings by component, deductions (TDS), and net pay. If a PDF has been generated, **Download PDF** fetches it directly (it authenticates with your session — don't try to open the payslip link directly in a new browser tab, it won't carry your login).

### 7.4 Generating a One-off Payslip
**Generate Payslip** lets you produce a single employee's payslip for a specific month/year outside the normal run flow — pick employee, month, and year, then **Generate**. Useful for off-cycle or catch-up payslips.

---

## 8. Onboarding

**Onboarding** page (view differs by role — as HR Admin you see the admin view described below; a plain employee instead sees only their own checklist).

### 8.1 Starting Onboarding for a New Hire
Click **Initiate Onboarding**, choose the **Employee** and a **Template** (templates are pre-configured in the system; there's currently no screen to create/edit templates yourself — ask whoever manages the system config if you need a new one). This creates a workflow with a set of tasks auto-generated from the template, each with a due date calculated from the employee's joining date.

### 8.2 Tracking Progress
The workflows table shows every active onboarding: employee, template, start date, days active, task-completion progress bar, and status (In Progress / Completed / Cancelled). Click any row (or **View Tasks**) to drill into the task checklist.

### 8.3 Working a Checklist
Inside a workflow's task list, each task shows its type, status, and due date.
- **Mark Complete** — for a normal task; you can add optional remarks.
- **Upload** — appears on **Document Upload** type tasks; pick a document type label (e.g. AADHAR, PAN, PHOTO) and choose the file. Uploading doesn't auto-complete the task — mark it complete afterward once you've verified the document.

---

## 9. HRMS Assistant (Chatbot)

A chat bubble sits in the bottom-right corner on every page once logged in. Click it to open, type a question, and press Enter (or the send button). It can answer things like salary/payslip queries conversationally. Type **"help"** to see what it currently supports.

---

## 10. Roles & Who Can See What

| Area | Employee | Manager | HR Admin |
|---|:---:|:---:|:---:|
| Dashboard, Attendance (view), My Leave, Onboarding (own) | ✅ | ✅ | ✅ |
| Employees (view/edit), Leave Approvals, Leave Balances | — | ✅ | ✅ |
| Departments, Designations, Branches, User Accounts, Payroll, Leave Admin, Leave Types | — | — | ✅ |
| Onboarding — initiate & manage all | — | — | ✅ |

Managers additionally see and approve leave for their direct reports; HR Admin sees and can act on everything company-wide.

---

## 11. Tips & Troubleshooting

- **Dropdown is empty when adding an employee** — almost always an ordering issue: make sure the Branch has that Department checked (Section 2, Step 2), and the Designation exists under that Department (Step 3).
- **Reporting Manager list is empty** — the manager candidate must (a) be in the *same department* as the employee being created, and (b) hold a designation flagged **Managerial**.
- **"No employee record linked" on My Leave / Approvals** — the logged-in user account isn't linked to an employee profile; create/verify the User Account's employee link (Section 4).
- **Can't back into CTC by "Monthly Basic"** — only works if Basic is configured as a plain percentage of gross; if Basic has been set to Fixed or chained to another component, switch to "Annual CTC" or "Monthly Gross" entry instead.
- **Salary components "don't add up to gross"** warning — either adjust the percentages/fixed amounts, or mark one earning component as **Formula** so it auto-absorbs the remainder.
- **Payslip PDF won't open** — always use the **Download PDF** button inside the app; a bare link opened in a new tab has no login token and will fail.
- **Unsaved changes** — closing any Add/Edit dialog with unsaved edits prompts a "Discard changes?" confirmation, so accidental closes won't silently lose data.

---

*This manual reflects the HRMS portal as of August 6, 2026. Screens and validation rules may evolve — if something here no longer matches what you see on screen, check with your system administrator.*

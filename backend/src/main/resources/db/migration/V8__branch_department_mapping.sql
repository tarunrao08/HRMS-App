-- A branch can offer several departments, and the same department can exist across several
-- branches, so the relationship is many-to-many via a plain join table (no extra attributes
-- needed on the association itself).
CREATE TABLE branch_departments (
    branch_id     UUID NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    department_id UUID NOT NULL REFERENCES departments(id) ON DELETE CASCADE,
    PRIMARY KEY (branch_id, department_id)
);

CREATE INDEX idx_branch_departments_department ON branch_departments (department_id);

-- Backfill: link every existing branch to every existing department so employee creation
-- keeps working immediately for current data. HR can narrow this down per branch afterwards
-- via the Branches page.
INSERT INTO branch_departments (branch_id, department_id)
SELECT b.id, d.id FROM branches b CROSS JOIN departments d;

-- Allow the same designation name in different departments.
-- Drop the global unique constraint on name, replace with per-department uniqueness.

ALTER TABLE designations DROP CONSTRAINT IF EXISTS designations_name_key;
ALTER TABLE designations DROP CONSTRAINT IF EXISTS designations_title_key;

CREATE UNIQUE INDEX uq_designation_name_department
    ON designations (LOWER(name), department_id)
    WHERE department_id IS NOT NULL;

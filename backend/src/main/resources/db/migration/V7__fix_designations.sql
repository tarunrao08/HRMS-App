-- Rename 'title' to 'name' and add optional department_id FK to designations

ALTER TABLE designations RENAME COLUMN title TO name;

ALTER TABLE designations
    ADD COLUMN department_id UUID REFERENCES departments(id) ON DELETE SET NULL;

CREATE INDEX idx_designations_department_id ON designations(department_id);

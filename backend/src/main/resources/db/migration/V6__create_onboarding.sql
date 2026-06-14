-- Onboarding templates, per-template task definitions, per-employee workflow instances, tasks, and document uploads; seeds the default 6-step template
CREATE TABLE onboarding_templates (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name           VARCHAR(100) NOT NULL UNIQUE,
    description    TEXT,
    department_id  UUID REFERENCES departments(id)  ON DELETE SET NULL,
    designation_id UUID REFERENCES designations(id) ON DELETE SET NULL,
    is_default     BOOLEAN NOT NULL DEFAULT FALSE,
    is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100)
);

CREATE TABLE onboarding_task_definitions (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_id          UUID         NOT NULL REFERENCES onboarding_templates(id) ON DELETE CASCADE,
    step_number          INT          NOT NULL,
    title                VARCHAR(200) NOT NULL,
    description          TEXT,
    task_type            VARCHAR(30)  NOT NULL,  -- DOCUMENT_UPLOAD | FORM_FILL | ACKNOWLEDGEMENT | IT_SETUP | TRAINING
    due_days_from_joining INT         NOT NULL DEFAULT 7,
    is_mandatory         BOOLEAN      NOT NULL DEFAULT TRUE,
    responsible_role     VARCHAR(50)  NOT NULL DEFAULT 'ROLE_EMPLOYEE',
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100)
);

CREATE TABLE onboarding_workflows (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id    UUID        NOT NULL UNIQUE REFERENCES employees(id)           ON DELETE CASCADE,
    template_id    UUID        NOT NULL        REFERENCES onboarding_templates(id) ON DELETE RESTRICT,
    status         VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    current_step   INT         NOT NULL DEFAULT 1,
    total_steps    INT         NOT NULL DEFAULT 6,
    started_at     TIMESTAMP WITH TIME ZONE,
    completed_at   TIMESTAMP WITH TIME ZONE,
    assigned_hr_id UUID REFERENCES employees(id) ON DELETE SET NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100)
);

CREATE TABLE onboarding_tasks (
    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workflow_id        UUID         NOT NULL REFERENCES onboarding_workflows(id)      ON DELETE CASCADE,
    task_definition_id UUID         NOT NULL REFERENCES onboarding_task_definitions(id) ON DELETE RESTRICT,
    step_number        INT          NOT NULL,
    title              VARCHAR(200) NOT NULL,
    description        TEXT,
    task_type          VARCHAR(30)  NOT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    due_date           DATE,
    completed_at       TIMESTAMP WITH TIME ZONE,
    completed_by       UUID REFERENCES users(id) ON DELETE SET NULL,
    notes              TEXT,
    document_url       TEXT,
    rejection_reason   TEXT,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(100),
    updated_by         VARCHAR(100)
);

CREATE TABLE onboarding_documents (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id     UUID         NOT NULL REFERENCES employees(id)        ON DELETE CASCADE,
    task_id         UUID REFERENCES onboarding_tasks(id) ON DELETE SET NULL,
    document_type   VARCHAR(50)  NOT NULL,
    document_name   VARCHAR(200) NOT NULL,
    file_url        TEXT         NOT NULL,
    file_size_bytes BIGINT,
    mime_type       VARCHAR(100),
    uploaded_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    is_verified     BOOLEAN      NOT NULL DEFAULT FALSE,
    verified_by     UUID REFERENCES users(id) ON DELETE SET NULL,
    verified_at     TIMESTAMP WITH TIME ZONE,
    rejection_reason TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

-- Default 6-step template applied to all employees unless overridden by department/designation
INSERT INTO onboarding_templates (name, description, is_default, is_active, created_by, updated_by)
VALUES ('Standard Onboarding', 'Default 6-step onboarding process for all new hires', TRUE, TRUE, 'system', 'system');

INSERT INTO onboarding_task_definitions
    (template_id, step_number, title, description, task_type, due_days_from_joining, is_mandatory, responsible_role, created_by, updated_by)
SELECT
    t.id,
    steps.step_number,
    steps.title,
    steps.description,
    steps.task_type,
    steps.due_days,
    TRUE,
    steps.role,
    'system',
    'system'
FROM onboarding_templates t,
     (VALUES
         (1, 'Personal Document Submission',    'Upload Aadhar, PAN, passport-size photo, and address proof',  'DOCUMENT_UPLOAD',  3,  'ROLE_EMPLOYEE'),
         (2, 'Bank Account Details',             'Submit bank account number, IFSC, and cancelled cheque',       'FORM_FILL',        3,  'ROLE_EMPLOYEE'),
         (3, 'IT Asset & Email Setup',           'Provision laptop, email ID, and access credentials',           'IT_SETUP',         2,  'ROLE_HR_ADMIN'),
         (4, 'Policy Acknowledgement',           'Read and e-sign HR policy handbook and NDA',                   'ACKNOWLEDGEMENT',  5,  'ROLE_EMPLOYEE'),
         (5, 'Induction Training',               'Complete mandatory induction and compliance training modules',  'TRAINING',         10, 'ROLE_EMPLOYEE'),
         (6, 'Profile Completion',               'Complete employee profile in the HRMS portal',                 'FORM_FILL',        7,  'ROLE_EMPLOYEE')
     ) AS steps(step_number, title, description, task_type, due_days, role)
WHERE t.is_default = TRUE;

CREATE INDEX idx_onboarding_workflows_employee ON onboarding_workflows(employee_id);
CREATE INDEX idx_onboarding_workflows_status   ON onboarding_workflows(status);
CREATE INDEX idx_onboarding_tasks_workflow_id  ON onboarding_tasks(workflow_id);
CREATE INDEX idx_onboarding_tasks_status       ON onboarding_tasks(status);
CREATE INDEX idx_onboarding_docs_employee_id   ON onboarding_documents(employee_id);

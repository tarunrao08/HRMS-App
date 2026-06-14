-- Leave types, per-employee balances, leave requests (multi-level), and approval audit trail; seeds Indian standard leave types
CREATE TABLE leave_types (
    id                     UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name                   VARCHAR(100) NOT NULL UNIQUE,
    code                   VARCHAR(10)  NOT NULL UNIQUE,
    description            TEXT,
    max_days_per_year      INT          NOT NULL DEFAULT 0,
    carry_forward_allowed  BOOLEAN      NOT NULL DEFAULT FALSE,
    max_carry_forward_days INT          NOT NULL DEFAULT 0,
    encashment_allowed     BOOLEAN      NOT NULL DEFAULT FALSE,
    is_paid                BOOLEAN      NOT NULL DEFAULT TRUE,
    requires_document      BOOLEAN      NOT NULL DEFAULT FALSE,
    min_notice_days        INT          NOT NULL DEFAULT 0,
    is_active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by             VARCHAR(100),
    updated_by             VARCHAR(100)
);

CREATE TABLE leave_balances (
    id                   UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id          UUID         NOT NULL REFERENCES employees(id)   ON DELETE CASCADE,
    leave_type_id        UUID         NOT NULL REFERENCES leave_types(id) ON DELETE RESTRICT,
    year                 INT          NOT NULL,
    allocated_days       DECIMAL(5,2) NOT NULL DEFAULT 0,
    used_days            DECIMAL(5,2) NOT NULL DEFAULT 0,
    pending_days         DECIMAL(5,2) NOT NULL DEFAULT 0,
    carried_forward_days DECIMAL(5,2) NOT NULL DEFAULT 0,
    lapsed_days          DECIMAL(5,2) NOT NULL DEFAULT 0,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100),
    UNIQUE (employee_id, leave_type_id, year)
);

CREATE TABLE leave_requests (
    id                  UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id         UUID         NOT NULL REFERENCES employees(id)   ON DELETE CASCADE,
    leave_type_id       UUID         NOT NULL REFERENCES leave_types(id) ON DELETE RESTRICT,
    start_date          DATE         NOT NULL,
    end_date            DATE         NOT NULL,
    total_days          DECIMAL(5,2) NOT NULL,
    half_day            BOOLEAN      NOT NULL DEFAULT FALSE,
    half_day_type       VARCHAR(10),           -- FIRST_HALF | SECOND_HALF
    reason              TEXT         NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    applied_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    document_url        TEXT,
    current_approver_id UUID REFERENCES employees(id) ON DELETE SET NULL,
    rejection_reason    TEXT,
    cancelled_at        TIMESTAMP WITH TIME ZONE,
    cancelled_by        UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100)
);

CREATE TABLE leave_approvals (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    leave_request_id UUID        NOT NULL REFERENCES leave_requests(id) ON DELETE CASCADE,
    approver_id      UUID        NOT NULL REFERENCES employees(id)      ON DELETE RESTRICT,
    approver_level   INT         NOT NULL DEFAULT 1,
    action           VARCHAR(20) NOT NULL,   -- APPROVED | REJECTED | FORWARDED
    comments         TEXT,
    acted_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100)
);

-- Standard Indian leave types
INSERT INTO leave_types (name, code, max_days_per_year, carry_forward_allowed, max_carry_forward_days,
                         encashment_allowed, is_paid, requires_document, min_notice_days, created_by, updated_by)
VALUES
    ('Casual Leave',    'CL',   12,  FALSE, 0,  FALSE, TRUE,  FALSE, 1,  'system', 'system'),
    ('Sick Leave',      'SL',   12,  FALSE, 0,  FALSE, TRUE,  FALSE, 0,  'system', 'system'),
    ('Earned Leave',    'EL',   21,  TRUE,  15, TRUE,  TRUE,  FALSE, 7,  'system', 'system'),
    ('Maternity Leave', 'ML',   182, FALSE, 0,  FALSE, TRUE,  TRUE,  30, 'system', 'system'),
    ('Paternity Leave', 'PL',   15,  FALSE, 0,  FALSE, TRUE,  FALSE, 7,  'system', 'system'),
    ('Loss of Pay',     'LOP',  365, FALSE, 0,  FALSE, FALSE, FALSE, 0,  'system', 'system');

CREATE INDEX idx_leave_requests_employee_id    ON leave_requests(employee_id);
CREATE INDEX idx_leave_requests_status         ON leave_requests(status);
CREATE INDEX idx_leave_requests_dates          ON leave_requests(start_date, end_date);
CREATE INDEX idx_leave_balances_employee_id    ON leave_balances(employee_id);
CREATE INDEX idx_leave_approvals_request_id    ON leave_approvals(leave_request_id);

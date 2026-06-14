-- Shifts, employee shift assignments, daily attendance records, and monthly summary aggregates
CREATE TABLE shifts (
    id                    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name                  VARCHAR(100) NOT NULL UNIQUE,
    start_time            TIME         NOT NULL,
    end_time              TIME         NOT NULL,
    grace_period_minutes  INT          NOT NULL DEFAULT 15,
    working_hours         DECIMAL(4,2) NOT NULL DEFAULT 8.0,
    is_night_shift        BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100)
);

CREATE TABLE employee_shifts (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id    UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    shift_id       UUID NOT NULL REFERENCES shifts(id)    ON DELETE RESTRICT,
    effective_from DATE NOT NULL,
    effective_to   DATE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100)
);

CREATE TABLE attendance_records (
    id                    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id           UUID         NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    attendance_date       DATE         NOT NULL,
    punch_in              TIMESTAMP WITH TIME ZONE,
    punch_out             TIMESTAMP WITH TIME ZONE,
    working_hours         DECIMAL(5,2),
    overtime_hours        DECIMAL(5,2) NOT NULL DEFAULT 0,
    status                VARCHAR(30)  NOT NULL DEFAULT 'ABSENT',
    punch_in_location     VARCHAR(200),
    punch_out_location    VARCHAR(200),
    punch_in_ip           VARCHAR(45),
    punch_out_ip          VARCHAR(45),
    shift_id              UUID REFERENCES shifts(id) ON DELETE SET NULL,
    remarks               TEXT,
    is_regularized        BOOLEAN      NOT NULL DEFAULT FALSE,
    regularization_reason TEXT,
    regularized_by        UUID REFERENCES users(id) ON DELETE SET NULL,
    regularized_at        TIMESTAMP WITH TIME ZONE,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),
    UNIQUE (employee_id, attendance_date)
);

CREATE TABLE attendance_monthly_summary (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id         UUID         NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    year                INT          NOT NULL,
    month               INT          NOT NULL,
    present_days        INT          NOT NULL DEFAULT 0,
    absent_days         INT          NOT NULL DEFAULT 0,
    late_days           INT          NOT NULL DEFAULT 0,
    half_days           INT          NOT NULL DEFAULT 0,
    overtime_hours      DECIMAL(7,2) NOT NULL DEFAULT 0,
    total_working_hours DECIMAL(7,2) NOT NULL DEFAULT 0,
    working_days        INT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    UNIQUE (employee_id, year, month)
);

-- Seed a standard day shift
INSERT INTO shifts (name, start_time, end_time, grace_period_minutes, working_hours, created_by, updated_by)
VALUES ('General Shift', '09:00', '18:00', 15, 9.0, 'system', 'system');

CREATE INDEX idx_attendance_records_employee_date ON attendance_records(employee_id, attendance_date);
CREATE INDEX idx_attendance_records_date          ON attendance_records(attendance_date);
CREATE INDEX idx_attendance_records_status        ON attendance_records(status);
CREATE INDEX idx_employee_shifts_employee_id      ON employee_shifts(employee_id);
CREATE INDEX idx_attendance_monthly_emp_ym        ON attendance_monthly_summary(employee_id, year, month);

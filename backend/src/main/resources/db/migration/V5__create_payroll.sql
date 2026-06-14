-- Payroll components catalogue, salary structures, payroll runs, payslips, and TDS declarations; seeds Indian statutory components
CREATE TABLE payroll_components (
    id               UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    name             VARCHAR(100) NOT NULL UNIQUE,
    code             VARCHAR(20)  NOT NULL UNIQUE,
    component_type   VARCHAR(20)  NOT NULL,  -- EARNING | DEDUCTION | STATUTORY
    calculation_type VARCHAR(20)  NOT NULL,  -- FIXED | PERCENTAGE | FORMULA
    value            DECIMAL(12,4),
    percentage_of    VARCHAR(50),            -- e.g. "CTC", "BASIC"
    is_taxable       BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    display_order    INT          NOT NULL DEFAULT 0,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100)
);

CREATE TABLE salary_structures (
    id                  UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id         UUID         NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    effective_from      DATE         NOT NULL,
    effective_to        DATE,
    ctc                 DECIMAL(14,2) NOT NULL,
    basic               DECIMAL(14,2) NOT NULL,
    hra                 DECIMAL(14,2) NOT NULL DEFAULT 0,
    special_allowance   DECIMAL(14,2) NOT NULL DEFAULT 0,
    medical_allowance   DECIMAL(14,2) NOT NULL DEFAULT 0,
    transport_allowance DECIMAL(14,2) NOT NULL DEFAULT 0,
    lta                 DECIMAL(14,2) NOT NULL DEFAULT 0,
    gross_salary        DECIMAL(14,2) NOT NULL,
    pf_employee         DECIMAL(14,2) NOT NULL DEFAULT 0,
    pf_employer         DECIMAL(14,2) NOT NULL DEFAULT 0,
    esi_employee        DECIMAL(14,2) NOT NULL DEFAULT 0,
    esi_employer        DECIMAL(14,2) NOT NULL DEFAULT 0,
    professional_tax    DECIMAL(14,2) NOT NULL DEFAULT 0,
    net_salary          DECIMAL(14,2) NOT NULL,
    is_active           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100)
);

CREATE TABLE payroll_runs (
    id               UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    year             INT          NOT NULL,
    month            INT          NOT NULL,
    run_date         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    status           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    total_employees  INT          NOT NULL DEFAULT 0,
    total_gross      DECIMAL(16,2) NOT NULL DEFAULT 0,
    total_deductions DECIMAL(16,2) NOT NULL DEFAULT 0,
    total_net        DECIMAL(16,2) NOT NULL DEFAULT 0,
    processed_by     UUID REFERENCES users(id) ON DELETE SET NULL,
    approved_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    approved_at      TIMESTAMP WITH TIME ZONE,
    remarks          TEXT,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    UNIQUE (year, month)
);

CREATE TABLE payslips (
    id                  UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    payroll_run_id      UUID         NOT NULL REFERENCES payroll_runs(id) ON DELETE CASCADE,
    employee_id         UUID         NOT NULL REFERENCES employees(id)    ON DELETE CASCADE,
    year                INT          NOT NULL,
    month               INT          NOT NULL,
    basic               DECIMAL(14,2) NOT NULL DEFAULT 0,
    hra                 DECIMAL(14,2) NOT NULL DEFAULT 0,
    special_allowance   DECIMAL(14,2) NOT NULL DEFAULT 0,
    medical_allowance   DECIMAL(14,2) NOT NULL DEFAULT 0,
    transport_allowance DECIMAL(14,2) NOT NULL DEFAULT 0,
    lta                 DECIMAL(14,2) NOT NULL DEFAULT 0,
    other_earnings      DECIMAL(14,2) NOT NULL DEFAULT 0,
    gross_salary        DECIMAL(14,2) NOT NULL DEFAULT 0,
    pf_deduction        DECIMAL(14,2) NOT NULL DEFAULT 0,
    esi_deduction       DECIMAL(14,2) NOT NULL DEFAULT 0,
    professional_tax    DECIMAL(14,2) NOT NULL DEFAULT 0,
    tds                 DECIMAL(14,2) NOT NULL DEFAULT 0,
    loan_deduction      DECIMAL(14,2) NOT NULL DEFAULT 0,
    advance_deduction   DECIMAL(14,2) NOT NULL DEFAULT 0,
    other_deductions    DECIMAL(14,2) NOT NULL DEFAULT 0,
    total_deductions    DECIMAL(14,2) NOT NULL DEFAULT 0,
    lop_days            DECIMAL(5,2)  NOT NULL DEFAULT 0,
    lop_amount          DECIMAL(14,2) NOT NULL DEFAULT 0,
    net_salary          DECIMAL(14,2) NOT NULL DEFAULT 0,
    working_days        INT           NOT NULL DEFAULT 0,
    paid_days           DECIMAL(5,2)  NOT NULL DEFAULT 0,
    pdf_url             TEXT,
    is_published        BOOLEAN       NOT NULL DEFAULT FALSE,
    published_at        TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    UNIQUE (payroll_run_id, employee_id)
);

CREATE TABLE tds_declarations (
    id               UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id      UUID         NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    financial_year   VARCHAR(9)   NOT NULL,   -- e.g. "2024-25"
    regime_type      VARCHAR(10)  NOT NULL DEFAULT 'NEW', -- NEW | OLD
    hra_exemption    DECIMAL(14,2) NOT NULL DEFAULT 0,
    section_80c      DECIMAL(14,2) NOT NULL DEFAULT 0,
    section_80d      DECIMAL(14,2) NOT NULL DEFAULT 0,
    section_80g      DECIMAL(14,2) NOT NULL DEFAULT 0,
    other_deductions DECIMAL(14,2) NOT NULL DEFAULT 0,
    declared_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    is_verified      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    UNIQUE (employee_id, financial_year)
);

-- Indian payroll statutory components
INSERT INTO payroll_components (name, code, component_type, calculation_type, value, percentage_of,
                                is_taxable, display_order, created_by, updated_by)
VALUES
    ('Basic Salary',          'BASIC',     'EARNING',    'PERCENTAGE', 40.00, 'CTC',   TRUE,  1, 'system', 'system'),
    ('House Rent Allowance',  'HRA',       'EARNING',    'PERCENTAGE', 20.00, 'CTC',   TRUE,  2, 'system', 'system'),
    ('Special Allowance',     'SPECIAL',   'EARNING',    'FIXED',      NULL,  NULL,    TRUE,  3, 'system', 'system'),
    ('Medical Allowance',     'MEDICAL',   'EARNING',    'FIXED',      1250,  NULL,    FALSE, 4, 'system', 'system'),
    ('Transport Allowance',   'TRANSPORT', 'EARNING',    'FIXED',      1600,  NULL,    FALSE, 5, 'system', 'system'),
    ('Provident Fund',        'PF',        'STATUTORY',  'PERCENTAGE', 12.00, 'BASIC', FALSE, 6, 'system', 'system'),
    ('ESI Employee',          'ESI_EMP',   'STATUTORY',  'PERCENTAGE', 0.75,  'GROSS', FALSE, 7, 'system', 'system'),
    ('Professional Tax',      'PT',        'DEDUCTION',  'FIXED',      200,   NULL,    FALSE, 8, 'system', 'system'),
    ('TDS',                   'TDS',       'DEDUCTION',  'FORMULA',    NULL,  NULL,    FALSE, 9, 'system', 'system');

CREATE INDEX idx_payslips_employee_id      ON payslips(employee_id);
CREATE INDEX idx_payslips_payroll_run_id   ON payslips(payroll_run_id);
CREATE INDEX idx_payslips_year_month       ON payslips(year, month);
CREATE INDEX idx_salary_structures_emp_id  ON salary_structures(employee_id);
CREATE INDEX idx_tds_declarations_emp_id   ON tds_declarations(employee_id);

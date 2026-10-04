-- Payroll components catalogue, salary structures (open-ended, catalog-driven line-item
-- model rather than fixed earning columns), payroll runs, payslips (with a mirrored
-- line-item breakdown), and TDS declarations; seeds Indian statutory components.
CREATE TABLE payroll_components (
    id                         UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    name                       VARCHAR(100) NOT NULL UNIQUE,
    code                       VARCHAR(20)  NOT NULL UNIQUE,
    component_type             VARCHAR(20)  NOT NULL,  -- EARNING | DEDUCTION | STATUTORY
    calculation_type           VARCHAR(20)  NOT NULL,  -- FIXED | PERCENTAGE | FORMULA
    value                      DECIMAL(12,4),
    -- When calculation_type is PERCENTAGE, the component this percentage is taken of.
    -- NULL means "percentage of monthly gross" (the default basis).
    percentage_of_component_id UUID REFERENCES payroll_components(id) ON DELETE SET NULL,
    is_taxable                 BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active                  BOOLEAN      NOT NULL DEFAULT TRUE,
    display_order              INT          NOT NULL DEFAULT 0,
    created_at                 TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by                 VARCHAR(100),
    updated_by                 VARCHAR(100)
);

CREATE TABLE salary_structures (
    id             UUID          PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id    UUID          NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    effective_from DATE          NOT NULL,
    effective_to   DATE,
    ctc            DECIMAL(14,2) NOT NULL,
    annual_ctc     DECIMAL(14,2) NOT NULL DEFAULT 0,
    pf_applicable  BOOLEAN       NOT NULL DEFAULT FALSE,
    esi_applicable BOOLEAN       NOT NULL DEFAULT FALSE,
    -- Earning breakdown (Basic, HRA, DA, ...) lives in salary_structure_components rows below,
    -- not as fixed columns here.
    gross_salary   DECIMAL(14,2) NOT NULL,
    pf_employee    DECIMAL(14,2) NOT NULL DEFAULT 0,
    pf_employer    DECIMAL(14,2) NOT NULL DEFAULT 0,
    esi_employee   DECIMAL(14,2) NOT NULL DEFAULT 0,
    esi_employer   DECIMAL(14,2) NOT NULL DEFAULT 0,
    professional_tax DECIMAL(14,2) NOT NULL DEFAULT 0,
    net_salary     DECIMAL(14,2) NOT NULL,
    is_active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100)
);

CREATE INDEX idx_salary_structures_emp_id ON salary_structures(employee_id);

-- Any active payroll_component can be attached to any employee's structure with its own
-- percentage/fixed value, with zero schema changes needed to add a new component later.
CREATE TABLE salary_structure_components (
    id                    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    salary_structure_id   UUID NOT NULL REFERENCES salary_structures(id)  ON DELETE CASCADE,
    payroll_component_id  UUID NOT NULL REFERENCES payroll_components(id) ON DELETE RESTRICT,
    calculation_type      VARCHAR(20) NOT NULL,
    value                 DECIMAL(12,4),
    computed_amount       DECIMAL(14,2) NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),
    UNIQUE (salary_structure_id, payroll_component_id)
);

CREATE INDEX idx_salary_structure_components_structure ON salary_structure_components(salary_structure_id);

CREATE TABLE payroll_runs (
    id               UUID          PRIMARY KEY DEFAULT uuid_generate_v4(),
    year             INT           NOT NULL,
    month            INT           NOT NULL,
    run_date         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    status           VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    total_employees  INT           NOT NULL DEFAULT 0,
    total_gross      DECIMAL(16,2) NOT NULL DEFAULT 0,
    total_deductions DECIMAL(16,2) NOT NULL DEFAULT 0,
    total_net        DECIMAL(16,2) NOT NULL DEFAULT 0,
    processed_by     UUID REFERENCES users(id) ON DELETE SET NULL,
    approved_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    approved_at      TIMESTAMP WITH TIME ZONE,
    processed_at     TIMESTAMP WITH TIME ZONE,
    disbursed_by     UUID REFERENCES users(id) ON DELETE SET NULL,
    disbursed_at     TIMESTAMP WITH TIME ZONE,
    remarks          TEXT,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    UNIQUE (year, month)
);

CREATE TABLE payslips (
    id               UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    payroll_run_id   UUID         NOT NULL REFERENCES payroll_runs(id) ON DELETE CASCADE,
    employee_id      UUID         NOT NULL REFERENCES employees(id)    ON DELETE CASCADE,
    year             INT          NOT NULL,
    month            INT          NOT NULL,
    -- Earning breakdown (Basic, HRA, DA, ...) lives in payslip_components rows below — mirrors
    -- whatever components were configured on the salary structure that generated this payslip.
    gross_salary     DECIMAL(14,2) NOT NULL DEFAULT 0,
    pf_deduction     DECIMAL(14,2) NOT NULL DEFAULT 0,
    esi_deduction    DECIMAL(14,2) NOT NULL DEFAULT 0,
    professional_tax DECIMAL(14,2) NOT NULL DEFAULT 0,
    tds              DECIMAL(14,2) NOT NULL DEFAULT 0,
    loan_deduction   DECIMAL(14,2) NOT NULL DEFAULT 0,
    advance_deduction DECIMAL(14,2) NOT NULL DEFAULT 0,
    other_deductions DECIMAL(14,2) NOT NULL DEFAULT 0,
    total_deductions DECIMAL(14,2) NOT NULL DEFAULT 0,
    lop_days         DECIMAL(5,2)  NOT NULL DEFAULT 0,
    lop_amount       DECIMAL(14,2) NOT NULL DEFAULT 0,
    net_salary       DECIMAL(14,2) NOT NULL DEFAULT 0,
    working_days     INT           NOT NULL DEFAULT 0,
    paid_days        DECIMAL(5,2)  NOT NULL DEFAULT 0,
    pdf_url          TEXT,
    is_published     BOOLEAN       NOT NULL DEFAULT FALSE,
    published_at     TIMESTAMP WITH TIME ZONE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100),
    UNIQUE (payroll_run_id, employee_id)
);

CREATE TABLE payslip_components (
    id                    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    payslip_id            UUID NOT NULL REFERENCES payslips(id)          ON DELETE CASCADE,
    payroll_component_id  UUID NOT NULL REFERENCES payroll_components(id) ON DELETE RESTRICT,
    amount                DECIMAL(14,2) NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),
    UNIQUE (payslip_id, payroll_component_id)
);

CREATE INDEX idx_payslips_employee_id    ON payslips(employee_id);
CREATE INDEX idx_payslips_payroll_run_id ON payslips(payroll_run_id);
CREATE INDEX idx_payslips_year_month     ON payslips(year, month);
CREATE INDEX idx_payslip_components_payslip ON payslip_components(payslip_id);

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

CREATE INDEX idx_tds_declarations_emp_id ON tds_declarations(employee_id);

-- Indian payroll statutory components, seeded with the current default earning structure:
--   BASIC       45% of gross
--   HRA         40% of Basic
--   DA/CONVEYANCE/MEDICAL/TRANSPORT   7.5% of Basic
--   SPECIAL     FORMULA — the residual/balancing component that reconciles the breakdown to gross
INSERT INTO payroll_components (name, code, component_type, calculation_type, value,
                                is_taxable, display_order, created_by, updated_by)
VALUES
    ('Basic Salary',          'BASIC',      'EARNING',   'PERCENTAGE', 45.00, TRUE,  1,  'system', 'system'),
    ('House Rent Allowance',  'HRA',        'EARNING',   'PERCENTAGE', 40.00, TRUE,  2,  'system', 'system'),
    ('Special Allowance',     'SPECIAL',    'EARNING',   'FORMULA',    NULL,  TRUE,  3,  'system', 'system'),
    ('Medical Allowance',     'MEDICAL',    'EARNING',   'PERCENTAGE', 7.50,  FALSE, 4,  'system', 'system'),
    ('Transport Allowance',   'TRANSPORT',  'EARNING',   'PERCENTAGE', 7.50,  FALSE, 5,  'system', 'system'),
    ('Provident Fund',        'PF',         'STATUTORY', 'PERCENTAGE', 12.00, FALSE, 6,  'system', 'system'),
    ('ESI Employee',          'ESI_EMP',    'STATUTORY', 'PERCENTAGE', 0.75,  FALSE, 7,  'system', 'system'),
    ('Professional Tax',      'PT',         'DEDUCTION', 'FIXED',      200,   FALSE, 8,  'system', 'system'),
    ('TDS',                   'TDS',        'DEDUCTION', 'FORMULA',    NULL,  FALSE, 9,  'system', 'system'),
    ('Dearness Allowance',    'DA',         'EARNING',   'PERCENTAGE', 7.50,  TRUE,  25, 'system', 'system'),
    ('Conveyance Allowance',  'CONVEYANCE', 'EARNING',   'PERCENTAGE', 7.50,  TRUE,  26, 'system', 'system');

UPDATE payroll_components
SET percentage_of_component_id = (SELECT id FROM payroll_components WHERE code = 'BASIC')
WHERE code IN ('HRA', 'PF', 'MEDICAL', 'TRANSPORT', 'DA', 'CONVEYANCE');

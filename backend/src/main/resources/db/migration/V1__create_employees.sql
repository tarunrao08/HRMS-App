-- Departments, designations, branches, and the core employees table with all audit fields
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE departments (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100)
);

CREATE TABLE designations (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title       VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100)
);

CREATE TABLE branches (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name       VARCHAR(100) NOT NULL UNIQUE,
    address    TEXT,
    city       VARCHAR(100),
    state      VARCHAR(100),
    country    VARCHAR(100) NOT NULL DEFAULT 'India',
    pincode    VARCHAR(10),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE TABLE employees (
    id                        UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_code             VARCHAR(20)  NOT NULL UNIQUE,
    first_name                VARCHAR(100) NOT NULL,
    last_name                 VARCHAR(100) NOT NULL,
    email                     VARCHAR(150) NOT NULL UNIQUE,
    phone                     VARCHAR(15),
    date_of_birth             DATE,
    gender                    VARCHAR(10),
    address                   TEXT,
    city                      VARCHAR(100),
    state                     VARCHAR(100),
    pincode                   VARCHAR(10),
    joining_date              DATE         NOT NULL,
    resignation_date          DATE,
    employment_status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    employment_type           VARCHAR(20)  NOT NULL DEFAULT 'FULL_TIME',
    department_id             UUID REFERENCES departments(id) ON DELETE SET NULL,
    designation_id            UUID REFERENCES designations(id) ON DELETE SET NULL,
    branch_id                 UUID REFERENCES branches(id) ON DELETE SET NULL,
    manager_id                UUID REFERENCES employees(id) ON DELETE SET NULL,
    profile_picture_url       TEXT,
    pan_number                VARCHAR(10),
    aadhar_number             VARCHAR(12),
    bank_account_number       VARCHAR(20),
    bank_ifsc_code            VARCHAR(11),
    bank_name                 VARCHAR(100),
    emergency_contact_name    VARCHAR(100),
    emergency_contact_phone   VARCHAR(15),
    emergency_contact_relation VARCHAR(50),
    created_at                TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by                VARCHAR(100),
    updated_by                VARCHAR(100)
);

CREATE INDEX idx_employees_email             ON employees(email);
CREATE INDEX idx_employees_employee_code     ON employees(employee_code);
CREATE INDEX idx_employees_department_id     ON employees(department_id);
CREATE INDEX idx_employees_designation_id    ON employees(designation_id);
CREATE INDEX idx_employees_manager_id        ON employees(manager_id);
CREATE INDEX idx_employees_employment_status ON employees(employment_status);
CREATE INDEX idx_employees_joining_date      ON employees(joining_date);

-- Employee family members and statutory nominees (PF/gratuity/insurance nomination)

CREATE TABLE employee_family_members (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id       UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    name              VARCHAR(150) NOT NULL,
    relationship      VARCHAR(20)  NOT NULL,
    date_of_birth     DATE,
    gender            VARCHAR(10),
    occupation        VARCHAR(100),
    contact_number    VARCHAR(15),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100)
);

CREATE INDEX idx_family_members_employee_id ON employee_family_members(employee_id);

CREATE TABLE employee_nominees (
    id                     UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id            UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    name                   VARCHAR(150) NOT NULL,
    relationship           VARCHAR(20)  NOT NULL,
    date_of_birth          DATE,
    share_percentage       NUMERIC(5,2) NOT NULL,
    address                TEXT,
    contact_number         VARCHAR(15),
    is_minor               BOOLEAN NOT NULL DEFAULT FALSE,
    guardian_name          VARCHAR(150),
    guardian_relationship  VARCHAR(20),
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by             VARCHAR(100),
    updated_by             VARCHAR(100),
    CONSTRAINT chk_nominee_share_percentage CHECK (share_percentage > 0 AND share_percentage <= 100)
);

CREATE INDEX idx_nominees_employee_id ON employee_nominees(employee_id);

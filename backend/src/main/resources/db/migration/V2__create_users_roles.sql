-- Users, roles, and the user_roles join table for authentication and RBAC; seeds the three default roles
CREATE TABLE roles (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(200),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100)
);

CREATE TABLE users (
    id                           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username                     VARCHAR(100) NOT NULL UNIQUE,
    password                     VARCHAR(255) NOT NULL,
    email                        VARCHAR(150) NOT NULL UNIQUE,
    employee_id                  UUID REFERENCES employees(id) ON DELETE SET NULL,
    is_enabled                   BOOLEAN NOT NULL DEFAULT TRUE,
    is_account_non_expired       BOOLEAN NOT NULL DEFAULT TRUE,
    is_account_non_locked        BOOLEAN NOT NULL DEFAULT TRUE,
    is_credentials_non_expired   BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at                TIMESTAMP WITH TIME ZONE,
    password_reset_token         VARCHAR(255),
    password_reset_token_expiry  TIMESTAMP WITH TIME ZONE,
    refresh_token                VARCHAR(500),
    refresh_token_expiry         TIMESTAMP WITH TIME ZONE,
    created_at                   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at                   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by                   VARCHAR(100),
    updated_by                   VARCHAR(100)
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Default roles seeded at startup; application code references these by name constant
INSERT INTO roles (name, description, created_by, updated_by) VALUES
    ('ROLE_HR_ADMIN', 'HR Administrator — full system access',               'system', 'system'),
    ('ROLE_MANAGER',  'Manager — team-level read/approve access',            'system', 'system'),
    ('ROLE_EMPLOYEE', 'Employee — self-service access only',                 'system', 'system');

CREATE INDEX idx_users_username    ON users(username);
CREATE INDEX idx_users_email       ON users(email);
CREATE INDEX idx_users_employee_id ON users(employee_id);

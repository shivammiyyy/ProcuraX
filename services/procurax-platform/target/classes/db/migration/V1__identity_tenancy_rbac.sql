-- Identity, tenancy and RBAC.

CREATE TABLE organizations (
    id          UUID PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  UUID,
    updated_by  UUID,
    version     BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE users (
    id               UUID PRIMARY KEY,
    email            VARCHAR(320) NOT NULL UNIQUE,
    full_name        VARCHAR(200) NOT NULL,
    auth_provider    VARCHAR(50)  NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_users_provider_subject UNIQUE (auth_provider, provider_subject)
);

CREATE TABLE roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE permissions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE role_permissions (
    role_id       UUID NOT NULL REFERENCES roles (id),
    permission_id UUID NOT NULL REFERENCES permissions (id),
    PRIMARY KEY (role_id, permission_id)
);

-- A user holds exactly one role per organization; the active organization
-- is always derived from the authenticated principal, never from the client.
CREATE TABLE organization_members (
    id              UUID PRIMARY KEY,
    organization_id UUID        NOT NULL REFERENCES organizations (id),
    user_id         UUID        NOT NULL REFERENCES users (id),
    role_id         UUID        NOT NULL REFERENCES roles (id),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_by      UUID,
    version         BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_member UNIQUE (organization_id, user_id)
);
CREATE INDEX idx_members_user ON organization_members (user_id);

INSERT INTO roles (name, description) VALUES
    ('PLATFORM_ADMIN', 'Platform operator'),
    ('ORG_ADMIN', 'Organization administrator'),
    ('PROCUREMENT_MANAGER', 'Manages procurement operations'),
    ('BUYER', 'Creates requests and RFQs'),
    ('APPROVER', 'Approves purchases'),
    ('FINANCE', 'Authorizes and captures payments'),
    ('VENDOR', 'External supplier'),
    ('VIEWER', 'Read-only access');

INSERT INTO permissions (name) VALUES
    ('USER_READ'), ('USER_MANAGE'),
    ('VENDOR_READ'), ('VENDOR_CREATE'), ('VENDOR_UPDATE'),
    ('RFQ_READ'), ('RFQ_CREATE'), ('RFQ_UPDATE'), ('RFQ_PUBLISH'),
    ('QUOTE_READ'), ('QUOTE_SUBMIT'), ('QUOTE_EVALUATE'),
    ('PO_READ'), ('PO_CREATE'), ('PO_APPROVE'),
    ('CONTRACT_READ'), ('CONTRACT_CREATE'), ('CONTRACT_APPROVE'),
    ('PAYMENT_READ'), ('PAYMENT_AUTHORIZE'), ('PAYMENT_CAPTURE'), ('PAYMENT_REFUND'),
    ('AGENT_EXECUTE'), ('AGENT_APPROVE'),
    ('AUDIT_READ');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name IN ('PLATFORM_ADMIN', 'ORG_ADMIN');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.name IN (
    'USER_READ', 'VENDOR_READ', 'VENDOR_CREATE', 'VENDOR_UPDATE',
    'RFQ_READ', 'RFQ_CREATE', 'RFQ_UPDATE', 'RFQ_PUBLISH',
    'QUOTE_READ', 'QUOTE_EVALUATE', 'PO_READ', 'PO_CREATE', 'PO_APPROVE',
    'CONTRACT_READ', 'CONTRACT_CREATE', 'CONTRACT_APPROVE',
    'PAYMENT_READ', 'AGENT_EXECUTE', 'AGENT_APPROVE', 'AUDIT_READ')
WHERE r.name = 'PROCUREMENT_MANAGER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.name IN (
    'VENDOR_READ', 'RFQ_READ', 'RFQ_CREATE', 'RFQ_UPDATE', 'RFQ_PUBLISH',
    'QUOTE_READ', 'QUOTE_EVALUATE', 'PO_READ', 'PO_CREATE',
    'CONTRACT_READ', 'CONTRACT_CREATE', 'PAYMENT_READ', 'AGENT_EXECUTE')
WHERE r.name = 'BUYER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.name IN (
    'VENDOR_READ', 'RFQ_READ', 'QUOTE_READ', 'PO_READ', 'PO_APPROVE',
    'CONTRACT_READ', 'CONTRACT_APPROVE', 'AGENT_APPROVE')
WHERE r.name = 'APPROVER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.name IN (
    'VENDOR_READ', 'PO_READ', 'PO_APPROVE', 'CONTRACT_READ',
    'PAYMENT_READ', 'PAYMENT_AUTHORIZE', 'PAYMENT_CAPTURE', 'PAYMENT_REFUND', 'AUDIT_READ')
WHERE r.name = 'FINANCE';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.name IN (
    'RFQ_READ', 'QUOTE_READ', 'QUOTE_SUBMIT', 'CONTRACT_READ')
WHERE r.name = 'VENDOR';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.name LIKE '%\_READ'
WHERE r.name = 'VIEWER' AND p.name <> 'AUDIT_READ';

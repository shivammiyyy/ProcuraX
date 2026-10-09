INSERT INTO permissions (name, description) VALUES
    ('APPROVAL_READ', 'Read organization approval requests'),
    ('APPROVAL_REQUEST', 'Submit purchases for human approval'),
    ('APPROVAL_DECIDE', 'Approve or reject assigned purchase requests')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
CROSS JOIN permissions permission
WHERE role.name IN ('PLATFORM_ADMIN', 'ORG_ADMIN')
  AND permission.name IN ('APPROVAL_READ', 'APPROVAL_REQUEST', 'APPROVAL_DECIDE')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
JOIN permissions permission ON permission.name IN ('APPROVAL_READ', 'APPROVAL_REQUEST')
WHERE role.name IN ('PROCUREMENT_MANAGER', 'BUYER')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
JOIN permissions permission ON permission.name IN ('APPROVAL_READ', 'APPROVAL_DECIDE')
WHERE role.name = 'APPROVER'
ON CONFLICT DO NOTHING;

CREATE TABLE approval_requests (
    id                    UUID PRIMARY KEY,
    organization_id       UUID NOT NULL REFERENCES organizations (id),
    requester_user_id     UUID NOT NULL REFERENCES users (id),
    subject               VARCHAR(160) NOT NULL,
    category              VARCHAR(100) NOT NULL,
    amount                NUMERIC(14, 2) NOT NULL CHECK (amount > 0),
    currency              VARCHAR(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    quotation_count       INT NOT NULL CHECK (quotation_count BETWEEN 0 AND 100),
    justification         VARCHAR(2000) NOT NULL,
    policy_evaluation_id  UUID NOT NULL,
    policy_decisions      JSONB NOT NULL,
    status                VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                          CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by            UUID,
    updated_by            UUID,
    version               BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_approval_request_tenant_id UNIQUE (organization_id, id),
    CONSTRAINT fk_approval_requester_membership
        FOREIGN KEY (organization_id, requester_user_id)
        REFERENCES organization_members (organization_id, user_id)
);

CREATE INDEX idx_approval_requests_org_created
    ON approval_requests (organization_id, created_at DESC);

CREATE TABLE approval_steps (
    id                UUID PRIMARY KEY,
    organization_id   UUID NOT NULL REFERENCES organizations (id),
    request_id        UUID NOT NULL,
    step_order        INT NOT NULL CHECK (step_order > 0),
    approver_user_id  UUID NOT NULL REFERENCES users (id),
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                      CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    decision_comment  VARCHAR(2000),
    decided_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        UUID,
    updated_by        UUID,
    version           BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_approval_step_request_tenant
        FOREIGN KEY (organization_id, request_id)
        REFERENCES approval_requests (organization_id, id),
    CONSTRAINT fk_approval_step_approver_membership
        FOREIGN KEY (organization_id, approver_user_id)
        REFERENCES organization_members (organization_id, user_id),
    CONSTRAINT uq_approval_step_order UNIQUE (organization_id, request_id, step_order),
    CONSTRAINT ck_approval_step_decision CHECK (
        (status = 'PENDING' AND decided_at IS NULL AND decision_comment IS NULL)
        OR (status IN ('APPROVED', 'REJECTED') AND decided_at IS NOT NULL)
    )
);

CREATE INDEX idx_approval_steps_assignee
    ON approval_steps (organization_id, approver_user_id, status);

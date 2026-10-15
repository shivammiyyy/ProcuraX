INSERT INTO permissions (name, description) VALUES
    ('POLICY_READ', 'Read organization purchasing policies'),
    ('POLICY_MANAGE', 'Create and update organization purchasing policies'),
    ('POLICY_EVALUATE', 'Evaluate proposed purchases against organization policies')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
CROSS JOIN permissions permission
WHERE role.name IN ('PLATFORM_ADMIN', 'ORG_ADMIN')
  AND permission.name IN ('POLICY_READ', 'POLICY_MANAGE', 'POLICY_EVALUATE')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
JOIN permissions permission ON permission.name IN ('POLICY_READ', 'POLICY_EVALUATE')
WHERE role.name IN ('PROCUREMENT_MANAGER', 'BUYER')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
JOIN permissions permission ON permission.name = 'POLICY_READ'
WHERE role.name = 'VIEWER'
ON CONFLICT DO NOTHING;

CREATE TABLE policy_rules (
    id                     UUID PRIMARY KEY,
    organization_id        UUID NOT NULL REFERENCES organizations (id),
    name                   VARCHAR(120) NOT NULL,
    rule_type              VARCHAR(40) NOT NULL,
    threshold_amount       NUMERIC(14, 2),
    allowed_currency_codes JSONB,
    minimum_quote_count    INT,
    enabled                BOOLEAN NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by             UUID,
    updated_by             UUID,
    version                BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_policy_rule_org_name UNIQUE (organization_id, name),
    CONSTRAINT ck_policy_rule_configuration CHECK (
        (rule_type IN ('MAX_PURCHASE_AMOUNT', 'APPROVAL_THRESHOLD')
            AND threshold_amount > 0
            AND allowed_currency_codes IS NULL
            AND minimum_quote_count IS NULL)
        OR
        (rule_type = 'ALLOWED_CURRENCIES'
            AND threshold_amount IS NULL
            AND jsonb_typeof(allowed_currency_codes) = 'array'
            AND jsonb_array_length(allowed_currency_codes) > 0
            AND minimum_quote_count IS NULL)
        OR
        (rule_type = 'MINIMUM_QUOTE_COUNT'
            AND threshold_amount IS NULL
            AND allowed_currency_codes IS NULL
            AND minimum_quote_count BETWEEN 2 AND 20)
    )
);

CREATE INDEX idx_policy_rules_org_enabled
    ON policy_rules (organization_id, enabled, name);

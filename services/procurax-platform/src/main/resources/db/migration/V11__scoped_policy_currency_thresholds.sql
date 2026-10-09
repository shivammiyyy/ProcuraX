ALTER TABLE policy_rules
    ADD COLUMN category VARCHAR(100),
    ADD COLUMN threshold_currency VARCHAR(3);

UPDATE policy_rules
SET threshold_currency = 'XXX',
    enabled = FALSE,
    updated_at = now()
WHERE rule_type IN ('MAX_PURCHASE_AMOUNT', 'APPROVAL_THRESHOLD');

ALTER TABLE policy_rules
    DROP CONSTRAINT uq_policy_rule_org_name,
    DROP CONSTRAINT ck_policy_rule_configuration,
    ADD CONSTRAINT ck_policy_rule_configuration CHECK (
        (rule_type IN ('MAX_PURCHASE_AMOUNT', 'APPROVAL_THRESHOLD')
            AND threshold_amount > 0
            AND threshold_currency ~ '^[A-Z]{3}$'
            AND allowed_currency_codes IS NULL
            AND minimum_quote_count IS NULL)
        OR
        (rule_type = 'ALLOWED_CURRENCIES'
            AND threshold_amount IS NULL
            AND threshold_currency IS NULL
            AND jsonb_typeof(allowed_currency_codes) = 'array'
            AND jsonb_array_length(allowed_currency_codes) > 0
            AND minimum_quote_count IS NULL)
        OR
        (rule_type = 'MINIMUM_QUOTE_COUNT'
            AND threshold_amount IS NULL
            AND threshold_currency IS NULL
            AND allowed_currency_codes IS NULL
            AND minimum_quote_count BETWEEN 2 AND 20)
    );

CREATE UNIQUE INDEX uq_policy_rule_org_name_ci
    ON policy_rules (organization_id, lower(name));

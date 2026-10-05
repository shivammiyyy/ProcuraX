ALTER TABLE vendor_documents
    ADD COLUMN verified_by UUID REFERENCES users (id),
    ADD COLUMN verified_at TIMESTAMPTZ,
    ADD COLUMN rejection_reason VARCHAR(1000),
    ADD CONSTRAINT ck_vendor_document_verification_status
        CHECK (verification_status IN ('UNVERIFIED', 'VERIFIED', 'REJECTED')),
    ADD CONSTRAINT ck_vendor_document_verification_metadata
        CHECK (
            (verification_status = 'UNVERIFIED' AND verified_by IS NULL AND verified_at IS NULL
                AND rejection_reason IS NULL)
            OR (verification_status = 'VERIFIED' AND verified_by IS NOT NULL AND verified_at IS NOT NULL
                AND rejection_reason IS NULL)
            OR (verification_status = 'REJECTED' AND verified_by IS NOT NULL AND verified_at IS NOT NULL
                AND rejection_reason IS NOT NULL)
        );

INSERT INTO permissions (name) VALUES
    ('VENDOR_DOCUMENT_SUBMIT'),
    ('VENDOR_DOCUMENT_VERIFY');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name = 'VENDOR'
  AND p.name = 'VENDOR_DOCUMENT_SUBMIT';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r CROSS JOIN permissions p
WHERE r.name IN ('PLATFORM_ADMIN', 'ORG_ADMIN', 'PROCUREMENT_MANAGER')
  AND p.name = 'VENDOR_DOCUMENT_VERIFY';

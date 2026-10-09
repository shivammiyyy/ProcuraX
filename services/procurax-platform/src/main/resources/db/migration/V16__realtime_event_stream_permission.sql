INSERT INTO permissions (name, description)
VALUES ('EVENT_STREAM_READ', 'Subscribe to organization procurement event updates')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
CROSS JOIN permissions permission
WHERE role.name IN (
    'PLATFORM_ADMIN', 'ORG_ADMIN', 'PROCUREMENT_MANAGER', 'BUYER', 'APPROVER', 'FINANCE', 'VIEWER'
)
  AND permission.name = 'EVENT_STREAM_READ'
ON CONFLICT DO NOTHING;

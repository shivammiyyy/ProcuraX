ALTER TABLE outbox_events
    ADD COLUMN replay_count INTEGER NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_outbox_replay_count CHECK (replay_count BETWEEN 0 AND 3);

INSERT INTO permissions (name, description)
VALUES ('OUTBOX_REPLAY', 'Replay dead-lettered organization events');

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
CROSS JOIN permissions permission
WHERE role.name = 'PLATFORM_ADMIN'
  AND permission.name = 'OUTBOX_REPLAY';

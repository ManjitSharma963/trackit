-- Ownership safety remediation:
-- If any schedule references a user row that does not exist, clear and remove it.
-- We prefer deleting unresolved records over silently reassigning ownership.
UPDATE schedules
SET user_id = NULL
WHERE user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM users u
      WHERE u.id = schedules.user_id
  );

DELETE FROM schedules WHERE user_id IS NULL;

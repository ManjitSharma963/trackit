-- Scope schedules per user. Legacy rows without owner are assigned to the smallest user id, then any still-null rows are removed.
SET @has_schedules_user_id := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'schedules'
      AND column_name = 'user_id'
);
SET @ddl := IF(@has_schedules_user_id = 0,
               'ALTER TABLE schedules ADD COLUMN user_id BIGINT NULL',
               'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE schedules SET user_id = (SELECT MIN(id) FROM users) WHERE user_id IS NULL;

DELETE FROM schedules
WHERE user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM users u
      WHERE u.id = schedules.user_id
  );

DELETE FROM schedules WHERE user_id IS NULL;

-- H2 (MODE=MySQL) and MySQL both accept MODIFY for NOT NULL
ALTER TABLE schedules MODIFY user_id BIGINT NOT NULL;

SET @has_fk_schedules_user := (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'schedules'
      AND constraint_name = 'fk_schedules_user'
      AND constraint_type = 'FOREIGN KEY'
);
SET @ddl := IF(@has_fk_schedules_user = 0,
               'ALTER TABLE schedules ADD CONSTRAINT fk_schedules_user FOREIGN KEY (user_id) REFERENCES users (id)',
               'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx_schedules_user_date := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'schedules'
      AND index_name = 'idx_schedules_user_date'
);
SET @ddl := IF(@has_idx_schedules_user_date = 0,
               'CREATE INDEX idx_schedules_user_date ON schedules (user_id, schedule_date)',
               'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- budget_plans may exist from an older schema without is_active; CREATE TABLE IF NOT EXISTS in V3 does not add columns.
SET @has_is_active := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'budget_plans'
      AND column_name = 'is_active'
);
SET @ddl := IF(@has_is_active = 0,
               'ALTER TABLE budget_plans ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT FALSE',
               'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

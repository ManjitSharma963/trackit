-- budget_categories may predate V3 or come from Hibernate ddl-auto with column `value` instead of `allocation_value`.
-- CREATE TABLE IF NOT EXISTS in V3 does not add/rename columns on an existing table.

SET @has_alloc := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'budget_categories'
      AND column_name = 'allocation_value'
);

SET @has_value := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'budget_categories'
      AND column_name = 'value'
);

SET @ddl := IF(@has_alloc > 0,
               'SELECT 1',
               IF(@has_value > 0,
                  'ALTER TABLE budget_categories ADD COLUMN allocation_value DECIMAL(14,4) NULL',
                  'ALTER TABLE budget_categories ADD COLUMN allocation_value DECIMAL(14,4) NOT NULL DEFAULT 0'));
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl2 := IF(@has_alloc = 0 AND @has_value > 0,
                'UPDATE budget_categories SET allocation_value = COALESCE(`value`, 0)',
                'SELECT 1');
PREPARE stmt2 FROM @ddl2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

SET @ddl3 := IF(@has_alloc = 0 AND @has_value > 0,
                'ALTER TABLE budget_categories MODIFY COLUMN allocation_value DECIMAL(14,4) NOT NULL',
                'SELECT 1');
PREPARE stmt3 FROM @ddl3;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;

SET @ddl4 := IF(@has_alloc = 0 AND @has_value > 0,
                'ALTER TABLE budget_categories DROP COLUMN `value`',
                'SELECT 1');
PREPARE stmt4 FROM @ddl4;
EXECUTE stmt4;
DEALLOCATE PREPARE stmt4;

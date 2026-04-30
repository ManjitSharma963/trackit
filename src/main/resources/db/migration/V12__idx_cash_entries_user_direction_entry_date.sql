-- Helps the new date-range expense query:
-- WHERE user_id = ? AND direction = ? AND entry_date between ? and ?
-- MySQL does not support CREATE INDEX IF NOT EXISTS reliably across versions,
-- so guard via information_schema.
SET @idx_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'cash_entries'
      AND index_name = 'idx_user_cash_direction_entry_date'
);

SET @create_idx_sql := IF(
    @idx_exists = 0,
    'CREATE INDEX idx_user_cash_direction_entry_date ON cash_entries (user_id, direction, entry_date)',
    'SELECT 1'
);

PREPARE stmt FROM @create_idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


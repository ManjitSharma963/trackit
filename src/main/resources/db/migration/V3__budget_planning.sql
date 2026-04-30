CREATE TABLE IF NOT EXISTS budget_plans (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    total_income DECIMAL(14, 2) NOT NULL,
    is_percentage BOOLEAN NOT NULL DEFAULT TRUE,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id)
);

SET @has_idx_budget_plans_user := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'budget_plans'
      AND index_name = 'idx_budget_plans_user'
);
SET @ddl := IF(@has_idx_budget_plans_user = 0,
               'CREATE INDEX idx_budget_plans_user ON budget_plans (user_id)',
               'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS budget_categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    type ENUM('NEED', 'WANT', 'SAVING') NOT NULL,
    allocation_value DECIMAL(14, 4) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (plan_id) REFERENCES budget_plans (id) ON DELETE CASCADE,
    UNIQUE KEY uq_budget_plan_category_name (plan_id, name)
);

SET @has_idx_budget_categories_plan := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'budget_categories'
      AND index_name = 'idx_budget_categories_plan'
);
SET @ddl := IF(@has_idx_budget_categories_plan = 0,
               'CREATE INDEX idx_budget_categories_plan ON budget_categories (plan_id)',
               'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_category_id := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'cash_entries'
      AND column_name = 'category_id'
);
SET @ddl := IF(@has_category_id = 0,
               'ALTER TABLE cash_entries ADD COLUMN category_id BIGINT NULL',
               'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_fk_cash_budget_category := (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'cash_entries'
      AND constraint_name = 'fk_cash_entry_budget_category'
      AND constraint_type = 'FOREIGN KEY'
);
SET @ddl := IF(@has_fk_cash_budget_category = 0,
               'ALTER TABLE cash_entries ADD CONSTRAINT fk_cash_entry_budget_category FOREIGN KEY (category_id) REFERENCES budget_categories (id)',
               'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx_cash_entries_user_category := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'cash_entries'
      AND index_name = 'idx_cash_entries_user_category'
);
SET @ddl := IF(@has_idx_cash_entries_user_category = 0,
               'CREATE INDEX idx_cash_entries_user_category ON cash_entries (user_id, category_id)',
               'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

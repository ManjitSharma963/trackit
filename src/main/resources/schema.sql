CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    type ENUM('CASH', 'BANK', 'UPI') NOT NULL,
    balance DECIMAL(14,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS notes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255),
    content TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_user_notes (user_id)
);

CREATE TABLE IF NOT EXISTS cash_entries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    direction ENUM('INCOME', 'EXPENSE') NOT NULL,
    title VARCHAR(255),
    amount DECIMAL(14,2) NOT NULL,
    category VARCHAR(100),
    notes TEXT,
    entry_date DATE NOT NULL,
    is_recurring BOOLEAN DEFAULT FALSE,
    recurrence_type ENUM('DAILY', 'WEEKLY', 'MONTHLY'),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (account_id) REFERENCES accounts(id),
    INDEX idx_user_cash_entries (user_id),
    INDEX idx_user_cash_direction (user_id, direction)
);

CREATE TABLE IF NOT EXISTS ledger (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    person_name VARCHAR(150) NOT NULL,
    type ENUM('GIVE', 'TAKE') NOT NULL,
    amount DECIMAL(14,2) NOT NULL,
    notes TEXT,
    transaction_date DATE NOT NULL,
    status ENUM('PENDING', 'SETTLED') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_user_ledger (user_id)
);

CREATE TABLE IF NOT EXISTS secrets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    username VARCHAR(150),
    password_encrypted TEXT NOT NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS schedules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    type ENUM('EVENT', 'TASK', 'REMINDER') NOT NULL,
    schedule_date DATE NOT NULL,
    schedule_time TIME NULL,
    status ENUM('PENDING', 'DONE') DEFAULT 'PENDING',
    notes TEXT,
    reminder_minutes INT DEFAULT 0,
    repeat_type ENUM('NONE', 'DAILY', 'WEEKLY', 'MONTHLY') DEFAULT 'NONE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);


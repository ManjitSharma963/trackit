ALTER TABLE ledger ADD COLUMN account_id BIGINT NULL;

ALTER TABLE ledger ADD CONSTRAINT fk_ledger_account FOREIGN KEY (account_id) REFERENCES accounts (id);

CREATE INDEX idx_ledger_account ON ledger (account_id);

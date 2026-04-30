ALTER TABLE cash_entries
    ADD COLUMN affects_balance BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE ledger
    ADD COLUMN loan_income_entry_id BIGINT NULL,
    ADD COLUMN loan_repay_entry_id BIGINT NULL;

ALTER TABLE ledger
    ADD CONSTRAINT fk_ledger_loan_income_entry
        FOREIGN KEY (loan_income_entry_id) REFERENCES cash_entries (id),
    ADD CONSTRAINT fk_ledger_loan_repay_entry
        FOREIGN KEY (loan_repay_entry_id) REFERENCES cash_entries (id);

CREATE INDEX idx_ledger_loan_income_entry ON ledger (loan_income_entry_id);
CREATE INDEX idx_ledger_loan_repay_entry ON ledger (loan_repay_entry_id);

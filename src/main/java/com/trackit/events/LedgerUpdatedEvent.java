package com.trackit.events;

import com.trackit.ledger.model.LedgerStatus;
import com.trackit.ledger.model.LedgerType;

import java.math.BigDecimal;

public record LedgerUpdatedEvent(LedgerBalanceSnapshot before, LedgerBalanceSnapshot after) {

    public record LedgerBalanceSnapshot(
            Long userId,
            LedgerStatus status,
            Long accountId,
            LedgerType type,
            BigDecimal amount) {}
}


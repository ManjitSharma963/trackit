package com.trackit.events;

import com.trackit.cash.model.CashDirection;

import java.math.BigDecimal;

public record CashEntryCreatedEvent(
        Long entryId,
        Long userId,
        Long accountId,
        CashDirection direction,
        BigDecimal amount) {}


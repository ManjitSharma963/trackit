package com.trackit.cash.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.scheduler.recurring-enabled", havingValue = "true", matchIfMissing = true)
public class CashEntryRecurringProcessor {

    private final RecurringService recurringService;

    public CashEntryRecurringProcessor(RecurringService recurringService) {
        this.recurringService = recurringService;
    }

    @Scheduled(cron = "${app.scheduler.recurring-cron:0 0 1 * * *}")
    public void materializeRecurringEntries() {
        recurringService.materializeRecurringEntries();
    }
}

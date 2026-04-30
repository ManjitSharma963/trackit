package com.trackit.cash.service;

import com.trackit.audit.AuditAction;
import com.trackit.audit.AuditLogService;
import com.trackit.cash.model.CashDirection;
import com.trackit.cash.model.CashEntry;
import com.trackit.common.RecurrenceType;
import com.trackit.cash.repository.CashEntryRepository;
import com.trackit.events.CashEntryCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class RecurringService {

    private final CashEntryRepository cashEntryRepository;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    public RecurringService(
            CashEntryRepository cashEntryRepository,
            AuditLogService auditLogService,
            ApplicationEventPublisher eventPublisher) {
        this.cashEntryRepository = cashEntryRepository;
        this.auditLogService = auditLogService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Generates the next dated row for all recurring cash entries due on {@code today},
     * and syncs account balances for the newly created rows. Idempotent per
     * (userId, accountId, direction, title, amount, nextEntryDate).
     */
    @Transactional
    public void materializeRecurringEntries(LocalDate today) {
        List<CashEntry> recurringToday = cashEntryRepository.findByRecurringTrueAndEntryDate(today);
        for (CashEntry source : recurringToday) {
            // Keep existing behavior: EXPENSE must be budget-linked unless it is label-only (categoryId == null).
            if (source.getDirection() == CashDirection.EXPENSE && source.getCategoryId() == null) {
                continue;
            }

            LocalDate nextDate = nextRecurrenceDate(today, source.getRecurrenceType());
            if (nextDate == null) {
                continue;
            }

            boolean exists = cashEntryRepository.existsByUserIdAndAccountIdAndDirectionAndTitleAndAmountAndEntryDate(
                    source.getUserId(),
                    source.getAccountId(),
                    source.getDirection(),
                    source.getTitle(),
                    source.getAmount(),
                    nextDate);
            if (exists) {
                continue;
            }

            CashEntry next = new CashEntry();
            next.setUserId(source.getUserId());
            next.setAccountId(source.getAccountId());
            next.setDirection(source.getDirection());
            next.setTitle(source.getTitle());
            next.setAmount(source.getAmount());
            next.setCategory(source.getCategory());
            next.setCategoryId(source.getCategoryId());
            next.setNotes(source.getNotes());
            next.setEntryDate(nextDate);
            next.setRecurring(true);
            next.setRecurrenceType(source.getRecurrenceType());

            CashEntry saved = cashEntryRepository.save(next);
            eventPublisher.publishEvent(new CashEntryCreatedEvent(
                    saved.getId(),
                    saved.getUserId(),
                    saved.getAccountId(),
                    saved.getDirection(),
                    saved.getAmount()));

            auditLogService.record(
                    saved.getUserId(),
                    "CASH_ENTRY",
                    saved.getId(),
                    AuditAction.CREATE,
                    null,
                    saved);
        }
    }

    @Transactional
    public void materializeRecurringEntries() {
        materializeRecurringEntries(LocalDate.now());
    }

    private static LocalDate nextRecurrenceDate(LocalDate date, RecurrenceType recurrenceType) {
        if (recurrenceType == null) {
            return null;
        }
        return switch (recurrenceType) {
            case DAILY -> date.plusDays(1);
            case WEEKLY -> date.plusWeeks(1);
            case MONTHLY -> date.plusMonths(1);
        };
    }
}


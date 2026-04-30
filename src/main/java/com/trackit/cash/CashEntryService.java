package com.trackit.cash.service;

import com.trackit.cash.dto.*;
import com.trackit.cash.model.*;
import com.trackit.cash.repository.*;

import com.trackit.account.Account;
import com.trackit.account.AccountBalanceService;
import com.trackit.account.AccountRepository;
import com.trackit.audit.AuditAction;
import com.trackit.audit.AuditLogService;
import com.trackit.auth.service.CurrentUserProvider;
import com.trackit.budget.service.BudgetService;
import com.trackit.common.PageResponse;
import com.trackit.common.RecurrenceType;
import com.trackit.common.ResourceNotFoundException;
import com.trackit.events.CashEntryCreatedEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class CashEntryService {

    private final CashEntryRepository cashEntryRepository;
    private final AccountRepository accountRepository;
    private final AccountBalanceService accountBalanceService;
    private final CurrentUserProvider currentUserProvider;
    private final BudgetService budgetService;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    public CashEntryService(
            CashEntryRepository cashEntryRepository,
            AccountRepository accountRepository,
            AccountBalanceService accountBalanceService,
            CurrentUserProvider currentUserProvider,
            BudgetService budgetService,
            AuditLogService auditLogService,
            ApplicationEventPublisher eventPublisher) {
        this.cashEntryRepository = cashEntryRepository;
        this.accountRepository = accountRepository;
        this.accountBalanceService = accountBalanceService;
        this.currentUserProvider = currentUserProvider;
        this.budgetService = budgetService;
        this.auditLogService = auditLogService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CashEntryResponse create(CashEntryRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        request.setUserId(userId);
        resolveAccountIdIfMissing(request, userId);
        validateRecurrence(request.isRecurring(), request.getRecurrenceType());
        budgetService.validateCashEntryBudgetLink(userId, request.getDirection(), request.getCategoryId());
        findAccountOrThrow(request.getAccountId(), userId);
        CashEntry entry = new CashEntry();
        applyRequest(entry, request);
        CashEntry saved = cashEntryRepository.save(entry);
        eventPublisher.publishEvent(new CashEntryCreatedEvent(
                saved.getId(),
                saved.getUserId(),
                saved.getAccountId(),
                saved.getDirection(),
                saved.getAmount()));
        auditLogService.record(
                userId,
                "CASH_ENTRY",
                saved.getId(),
                AuditAction.CREATE,
                null,
                saved);
        return toResponse(saved);
    }

    public PageResponse<CashEntryResponse> list(CashDirection direction, int page, int size) {
        Long userId = currentUserProvider.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("entryDate"), Sort.Order.desc("id")));
        Page<CashEntry> rows = direction == null
                ? cashEntryRepository.findByUserId(userId, pageable)
                : cashEntryRepository.findByUserIdAndDirection(userId, direction, pageable);
        return new PageResponse<>(
                rows.getContent().stream().map(this::toResponse).toList(),
                rows.getNumber(),
                rows.getSize(),
                rows.getTotalElements(),
                rows.getTotalPages());
    }

    public CashEntrySummaryResponse summarize() {
        Long userId = currentUserProvider.getCurrentUserId();
        BigDecimal income = cashEntryRepository.sumAmountByUserIdAndDirection(userId, CashDirection.INCOME);
        BigDecimal expense = cashEntryRepository.sumAmountByUserIdAndDirection(userId, CashDirection.EXPENSE);
        BigDecimal net = income.subtract(expense);
        return new CashEntrySummaryResponse(income, expense, net);
    }

    public CashEntryResponse getById(Long id) {
        CashEntry entry = findOwnedOrThrow(id);
        return toResponse(entry);
    }

    public List<CashEntryResponse> listByDateRange(
            CashDirection type, LocalDate fromInclusive, LocalDate toInclusive) {
        Long userId = currentUserProvider.getCurrentUserId();
        if (fromInclusive.isAfter(toInclusive)) {
            throw new IllegalArgumentException("`from` must be <= `to`.");
        }
        List<CashEntry> rows =
                cashEntryRepository.findByUserIdAndDirectionAndEntryDateBetweenOrderByEntryDateDescIdDesc(
                        userId, type, fromInclusive, toInclusive);
        return rows.stream().map(this::toResponse).toList();
    }

    @Transactional
    public CashEntryResponse update(Long id, CashEntryRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        request.setUserId(userId);
        CashEntry existing = findOwnedOrThrow(id);
        if (request.getAccountId() == null) {
            request.setAccountId(existing.getAccountId());
        }
        validateRecurrence(request.isRecurring(), request.getRecurrenceType());
        budgetService.validateCashEntryBudgetLink(userId, request.getDirection(), request.getCategoryId());
        // Snapshot old state before any mutation so "old value" stays correct.
        CashEntry oldSnapshot = snapshot(existing);
        accountBalanceService.reverseCashEntry(existing);
        applyRequest(existing, request);
        findAccountOrThrow(existing.getAccountId(), existing.getUserId());
        CashEntry saved = cashEntryRepository.save(existing);
        accountBalanceService.applyCashEntry(saved);
        auditLogService.record(
                userId,
                "CASH_ENTRY",
                saved.getId(),
                AuditAction.UPDATE,
                oldSnapshot,
                saved);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        CashEntry existing = findOwnedOrThrow(id);
        CashEntry oldSnapshot = snapshot(existing);
        accountBalanceService.reverseCashEntry(existing);
        cashEntryRepository.delete(existing);
        auditLogService.record(
                oldSnapshot.getUserId(),
                "CASH_ENTRY",
                oldSnapshot.getId(),
                AuditAction.DELETE,
                oldSnapshot,
                null);
    }

    /**
     * When client omits {@code accountId}, prefer JWT {@code accountId} claim; if absent/stale, use newest account.
     */
    private void resolveAccountIdIfMissing(CashEntryRequest request, Long userId) {
        if (request.getAccountId() != null) {
            return;
        }
        Long tokenAccountId = currentUserProvider.getCurrentAccountIdOrNull();
        if (tokenAccountId != null) {
            try {
                findAccountOrThrow(tokenAccountId, userId);
                request.setAccountId(tokenAccountId);
                return;
            } catch (IllegalArgumentException ignored) {
                // Token may contain a stale account id. Fall back to repository default selection.
            }
        }
        List<Account> owned = accountRepository.findByUserIdOrderByIdDesc(userId);
        if (owned.isEmpty()) {
            throw new IllegalArgumentException(
                    "No account found for this user. Create one with POST /api/accounts or complete signup.");
        }
        request.setAccountId(owned.get(0).getId());
    }

    private CashEntry findOwnedOrThrow(Long id) {
        Long userId = currentUserProvider.getCurrentUserId();
        CashEntry entry = cashEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cash entry not found with id: " + id));
        if (!entry.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Cash entry not found with id: " + id);
        }
        return entry;
    }

    private Account findAccountOrThrow(Long accountId, Long userId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No account with id " + accountId + ". Create one with POST /api/accounts and use its id."));
        if (!account.getUserId().equals(userId)) {
            throw new IllegalArgumentException(
                    "Account " + accountId + " does not belong to the signed-in user. Use an account id from GET /api/accounts.");
        }
        return account;
    }

    private void validateRecurrence(boolean recurring, RecurrenceType recurrenceType) {
        if (recurring && recurrenceType == null) {
            throw new IllegalArgumentException("recurrenceType is required when recurring is true");
        }
        if (!recurring && recurrenceType != null) {
            throw new IllegalArgumentException("recurrenceType must be null when recurring is false");
        }
    }

    private void applyRequest(CashEntry entry, CashEntryRequest request) {
        entry.setUserId(request.getUserId());
        entry.setAccountId(request.getAccountId());
        entry.setDirection(request.getDirection());
        entry.setTitle(request.getTitle());
        entry.setAmount(request.getAmount());
        entry.setCategory(request.getCategory());
        entry.setCategoryId(request.getCategoryId());
        entry.setNotes(request.getNotes());
        entry.setEntryDate(request.getEntryDate());
        entry.setRecurring(request.isRecurring());
        if (entry.getId() == null) {
            entry.setAffectsBalance(true);
        }
        entry.setRecurrenceType(request.getRecurrenceType());
    }

    private CashEntryResponse toResponse(CashEntry entry) {
        CashEntryResponse response = new CashEntryResponse();
        response.setId(entry.getId());
        response.setUserId(entry.getUserId());
        response.setAccountId(entry.getAccountId());
        response.setDirection(entry.getDirection());
        response.setTitle(entry.getTitle());
        response.setAmount(entry.getAmount());
        response.setCategory(entry.getCategory());
        response.setCategoryId(entry.getCategoryId());
        response.setNotes(entry.getNotes());
        response.setEntryDate(entry.getEntryDate());
        response.setRecurring(entry.isRecurring());
        response.setRecurrenceType(entry.getRecurrenceType());
        response.setCreatedAt(entry.getCreatedAt());
        return response;
    }

    private static CashEntry snapshot(CashEntry entry) {
        CashEntry c = new CashEntry();
        c.setId(entry.getId());
        c.setUserId(entry.getUserId());
        c.setAccountId(entry.getAccountId());
        c.setCategoryId(entry.getCategoryId());
        c.setDirection(entry.getDirection());
        c.setTitle(entry.getTitle());
        c.setAmount(entry.getAmount());
        c.setCategory(entry.getCategory());
        c.setNotes(entry.getNotes());
        c.setEntryDate(entry.getEntryDate());
        c.setRecurring(entry.isRecurring());
        c.setAffectsBalance(entry.isAffectsBalance());
        c.setRecurrenceType(entry.getRecurrenceType());
        return c;
    }
}

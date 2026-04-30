package com.trackit.ledger.service;

import com.trackit.account.AccountBalanceService;
import com.trackit.account.AccountRepository;
import com.trackit.audit.AuditAction;
import com.trackit.audit.AuditLogService;
import com.trackit.auth.service.CurrentUserProvider;
import com.trackit.cash.model.CashDirection;
import com.trackit.cash.model.CashEntry;
import com.trackit.cash.repository.CashEntryRepository;
import com.trackit.common.PageResponse;
import com.trackit.common.ResourceNotFoundException;
import com.trackit.events.LedgerUpdatedEvent;
import com.trackit.ledger.dto.LedgerRequest;
import com.trackit.ledger.dto.LedgerResponse;
import com.trackit.ledger.model.LedgerEntry;
import com.trackit.ledger.model.LedgerStatus;
import com.trackit.ledger.model.LedgerType;
import com.trackit.ledger.repository.LedgerRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class LedgerService {

    private final LedgerRepository ledgerRepository;
    private final AccountBalanceService accountBalanceService;
    private final CurrentUserProvider currentUserProvider;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;
    private final CashEntryRepository cashEntryRepository;
    private final AccountRepository accountRepository;

    public LedgerService(
            LedgerRepository ledgerRepository,
            AccountBalanceService accountBalanceService,
            CurrentUserProvider currentUserProvider,
            AuditLogService auditLogService,
            ApplicationEventPublisher eventPublisher,
            CashEntryRepository cashEntryRepository,
            AccountRepository accountRepository) {
        this.ledgerRepository = ledgerRepository;
        this.accountBalanceService = accountBalanceService;
        this.currentUserProvider = currentUserProvider;
        this.auditLogService = auditLogService;
        this.eventPublisher = eventPublisher;
        this.cashEntryRepository = cashEntryRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public LedgerResponse create(LedgerRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        LedgerEntry entry = new LedgerEntry();
        applyRequest(entry, request, userId);
        validateSettledHasAccount(entry);
        LedgerEntry saved = ledgerRepository.save(entry);
        syncLedgerAccountEffects(SettlementSnapshot.none(), saved);
        syncLoanCashEntries(snapshot(new LedgerEntry()), saved);
        auditLogService.record(
                userId,
                "LEDGER_ENTRY",
                saved.getId(),
                AuditAction.CREATE,
                null,
                saved);
        return toResponse(saved);
    }

    public PageResponse<LedgerResponse> list(int page, int size) {
        Long userId = currentUserProvider.getCurrentUserId();
        Page<LedgerEntry> result = ledgerRepository.findByUserId(
                userId,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("transactionDate"), Sort.Order.desc("id"))));
        return new PageResponse<>(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    public LedgerResponse getById(Long id) {
        return toResponse(findOwnedOrThrow(id));
    }

    @Transactional
    public LedgerResponse update(Long id, LedgerRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        LedgerEntry existing = findOwnedOrThrow(id);
        SettlementSnapshot before = SettlementSnapshot.from(existing);
        LedgerEntry oldSnapshot = snapshot(existing);
        applyRequest(existing, request, userId);
        validateSettledHasAccount(existing);
        syncLoanCashEntries(oldSnapshot, existing);
        LedgerEntry saved = ledgerRepository.save(existing);
        eventPublisher.publishEvent(new LedgerUpdatedEvent(
                new LedgerUpdatedEvent.LedgerBalanceSnapshot(
                        before.userId(),
                        before.status(),
                        before.accountId(),
                        before.type(),
                        before.amount()),
                new LedgerUpdatedEvent.LedgerBalanceSnapshot(
                        saved.getUserId(),
                        saved.getStatus(),
                        saved.getAccountId(),
                        saved.getType(),
                        saved.getAmount())));
        auditLogService.record(
                userId,
                "LEDGER_ENTRY",
                saved.getId(),
                AuditAction.UPDATE,
                oldSnapshot,
                saved);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        LedgerEntry entry = findOwnedOrThrow(id);
        LedgerEntry oldSnapshot = snapshot(entry);
        SettlementSnapshot snap = SettlementSnapshot.from(entry);
        accountBalanceService.reverseLedger(snap.toLedgerEntry());
        removeLinkedCashEntry(entry.getLoanIncomeEntryId(), entry.getUserId());
        removeLinkedCashEntry(entry.getLoanRepayEntryId(), entry.getUserId());
        ledgerRepository.delete(entry);
        auditLogService.record(
                oldSnapshot.getUserId(),
                "LEDGER_ENTRY",
                oldSnapshot.getId(),
                AuditAction.DELETE,
                oldSnapshot,
                null);
    }

    private static LedgerEntry snapshot(LedgerEntry entry) {
        LedgerEntry e = new LedgerEntry();
        e.setId(entry.getId());
        e.setUserId(entry.getUserId());
        e.setPersonName(entry.getPersonName());
        e.setType(entry.getType());
        e.setAmount(entry.getAmount());
        e.setNotes(entry.getNotes());
        e.setTransactionDate(entry.getTransactionDate());
        e.setStatus(entry.getStatus());
        e.setAccountId(entry.getAccountId());
        e.setLoanIncomeEntryId(entry.getLoanIncomeEntryId());
        e.setLoanRepayEntryId(entry.getLoanRepayEntryId());
        return e;
    }

    private void syncLoanCashEntries(LedgerEntry before, LedgerEntry after) {
        removeLinkedCashEntry(before.getLoanIncomeEntryId(), before.getUserId());
        removeLinkedCashEntry(before.getLoanRepayEntryId(), before.getUserId());
        after.setLoanIncomeEntryId(null);
        after.setLoanRepayEntryId(null);

        Long accountIdForCash = resolveAccountIdForLoanCash(after);
        if (after.getType() == LedgerType.TAKE) {
            CashEntry income = createLoanCashEntry(after, accountIdForCash, CashDirection.INCOME, "Loan", "Borrowed loan");
            after.setLoanIncomeEntryId(income.getId());
            return;
        }
        CashEntry repay = createLoanCashEntry(after, accountIdForCash, CashDirection.EXPENSE, "Loan Repay", "Loan repayment");
        after.setLoanRepayEntryId(repay.getId());
    }

    private CashEntry createLoanCashEntry(
            LedgerEntry ledger, Long accountId, CashDirection direction, String category, String titlePrefix) {
        CashEntry entry = new CashEntry();
        entry.setUserId(ledger.getUserId());
        entry.setAccountId(accountId);
        entry.setDirection(direction);
        entry.setTitle(titlePrefix + " - " + ledger.getPersonName());
        entry.setAmount(ledger.getAmount());
        entry.setCategory(category);
        entry.setNotes(ledger.getNotes());
        entry.setEntryDate(ledger.getTransactionDate());
        entry.setRecurring(false);
        entry.setAffectsBalance(false);
        return cashEntryRepository.save(entry);
    }

    private Long resolveAccountIdForLoanCash(LedgerEntry ledger) {
        if (ledger.getAccountId() != null) {
            return ledger.getAccountId();
        }
        Long tokenAccountId = currentUserProvider.getCurrentAccountIdOrNull();
        if (tokenAccountId != null && accountRepository.findById(tokenAccountId)
                .map(a -> ledger.getUserId().equals(a.getUserId()))
                .orElse(false)) {
            return tokenAccountId;
        }
        List<com.trackit.account.Account> owned = accountRepository.findByUserIdOrderByIdDesc(ledger.getUserId());
        if (owned.isEmpty()) {
            throw new IllegalArgumentException("No account found for ledger user; create account before adding ledger.");
        }
        return owned.get(0).getId();
    }

    private void removeLinkedCashEntry(Long cashEntryId, Long userId) {
        if (cashEntryId == null || userId == null) {
            return;
        }
        cashEntryRepository.findById(cashEntryId).ifPresent((entry) -> {
            if (userId.equals(entry.getUserId())) {
                cashEntryRepository.delete(entry);
            }
        });
    }

    private LedgerEntry findOwnedOrThrow(Long id) {
        Long userId = currentUserProvider.getCurrentUserId();
        LedgerEntry entry = ledgerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ledger entry not found with id: " + id));
        if (!entry.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Ledger entry not found with id: " + id);
        }
        return entry;
    }

    private void validateSettledHasAccount(LedgerEntry entry) {
        if (entry.getStatus() == LedgerStatus.SETTLED && entry.getAccountId() == null) {
            throw new IllegalArgumentException(
                    "accountId is required when status is SETTLED so the linked account balance can be updated.");
        }
    }

    private void syncLedgerAccountEffects(SettlementSnapshot before, LedgerEntry after) {
        accountBalanceService.reverseLedger(before.toLedgerEntry());
        accountBalanceService.applyLedger(after);
    }

    private void applyRequest(LedgerEntry entry, LedgerRequest request, Long userId) {
        entry.setUserId(userId);
        entry.setPersonName(request.getPersonName());
        entry.setType(request.getType());
        entry.setAmount(request.getAmount());
        entry.setNotes(request.getNotes());
        entry.setTransactionDate(request.getTransactionDate());
        entry.setStatus(request.getStatus() == null ? LedgerStatus.PENDING : request.getStatus());
        Long accountId = request.getAccountId();
        if (entry.getStatus() == LedgerStatus.SETTLED && accountId == null) {
            accountId = currentUserProvider.getCurrentAccountIdOrNull();
        }
        entry.setAccountId(accountId);
    }

    private LedgerResponse toResponse(LedgerEntry entry) {
        LedgerResponse response = new LedgerResponse();
        response.setId(entry.getId());
        response.setUserId(entry.getUserId());
        response.setPersonName(entry.getPersonName());
        response.setType(entry.getType());
        response.setAmount(entry.getAmount());
        response.setNotes(entry.getNotes());
        response.setTransactionDate(entry.getTransactionDate());
        response.setStatus(entry.getStatus());
        response.setAccountId(entry.getAccountId());
        response.setCreatedAt(entry.getCreatedAt());
        return response;
    }

    private record SettlementSnapshot(
            LedgerStatus status, Long accountId, LedgerType type, BigDecimal amount, Long userId) {

        static SettlementSnapshot none() {
            return new SettlementSnapshot(LedgerStatus.PENDING, null, null, null, null);
        }

        static SettlementSnapshot from(LedgerEntry e) {
            return new SettlementSnapshot(e.getStatus(), e.getAccountId(), e.getType(), e.getAmount(), e.getUserId());
        }

        LedgerEntry toLedgerEntry() {
            LedgerEntry e = new LedgerEntry();
            e.setStatus(status);
            e.setAccountId(accountId);
            e.setType(type);
            e.setAmount(amount);
            e.setUserId(userId);
            return e;
        }
    }
}

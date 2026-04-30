package com.trackit.account;

import com.trackit.auth.service.CurrentUserProvider;
import com.trackit.common.ResourceNotFoundException;
import com.trackit.audit.AuditAction;
import com.trackit.audit.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AuditLogService auditLogService;

    public AccountService(
            AccountRepository accountRepository,
            CurrentUserProvider currentUserProvider,
            AuditLogService auditLogService) {
        this.accountRepository = accountRepository;
        this.currentUserProvider = currentUserProvider;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public AccountResponse create(AccountRequest request) {
        request.setUserId(currentUserProvider.getCurrentUserId());
        Account account = new Account();
        applyRequest(account, request);
        Account saved = accountRepository.save(account);
        auditLogService.record(
                saved.getUserId(), "ACCOUNT", saved.getId(), AuditAction.CREATE, null, saved);
        return toResponse(saved);
    }

    /**
     * Creates the user's first wallet during signup (no JWT context). Same row shape as {@link #create}.
     */
    @Transactional
    public Long createDefaultAccountForNewUser(long userId) {
        Account account = new Account();
        account.setUserId(userId);
        account.setName("Cash");
        account.setType(AccountType.CASH);
        account.setBalance(BigDecimal.ZERO);
        Account saved = accountRepository.save(account);
        auditLogService.record(
                saved.getUserId(), "ACCOUNT", saved.getId(), AuditAction.CREATE, null, saved);
        return saved.getId();
    }

    /**
     * Same rule as default account auto-selection in cash entries: newest account row for this user.
     */
    public Long getDefaultAccountIdForUser(long userId) {
        return accountRepository.findByUserIdOrderByIdDesc(userId).stream()
                .findFirst()
                .map(Account::getId)
                .orElse(null);
    }

    public List<AccountResponse> list() {
        Long userId = currentUserProvider.getCurrentUserId();
        return accountRepository.findByUserIdOrderByIdDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AccountResponse getById(Long id) {
        return toResponse(findOwnedOrThrow(id));
    }

    @Transactional
    public AccountResponse update(Long id, AccountRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        request.setUserId(userId);
        Account account = findOwnedOrThrow(id);
        Account oldSnapshot = snapshot(account);
        applyRequest(account, request);
        Account saved = accountRepository.save(account);
        auditLogService.record(
                userId, "ACCOUNT", saved.getId(), AuditAction.UPDATE, oldSnapshot, saved);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Account existing = findOwnedOrThrow(id);
        Account oldSnapshot = snapshot(existing);
        accountRepository.delete(existing);
        auditLogService.record(
                oldSnapshot.getUserId(), "ACCOUNT", oldSnapshot.getId(), AuditAction.DELETE, oldSnapshot, null);
    }

    private Account findOwnedOrThrow(Long id) {
        Long userId = currentUserProvider.getCurrentUserId();
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
        if (!account.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Account not found with id: " + id);
        }
        return account;
    }

    private void applyRequest(Account account, AccountRequest request) {
        account.setUserId(request.getUserId());
        account.setName(request.getName());
        account.setType(request.getType());
        account.setBalance(request.getBalance());
    }

    private AccountResponse toResponse(Account account) {
        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setUserId(account.getUserId());
        response.setName(account.getName());
        response.setType(account.getType());
        response.setBalance(account.getBalance());
        response.setCreatedAt(account.getCreatedAt());
        return response;
    }

    private static Account snapshot(Account account) {
        Account a = new Account();
        a.setId(account.getId());
        a.setUserId(account.getUserId());
        a.setName(account.getName());
        a.setType(account.getType());
        a.setBalance(account.getBalance());
        return a;
    }
}

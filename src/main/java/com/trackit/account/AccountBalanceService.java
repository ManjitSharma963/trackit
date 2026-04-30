package com.trackit.account;

import com.trackit.cash.model.CashDirection;
import com.trackit.cash.model.CashEntry;
import com.trackit.common.ResourceNotFoundException;
import com.trackit.ledger.model.LedgerEntry;
import com.trackit.ledger.model.LedgerStatus;
import com.trackit.ledger.model.LedgerType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class AccountBalanceService {

    private final AccountRepository accountRepository;

    public AccountBalanceService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void applyCashEntry(CashEntry entry) {
        if (!entry.isAffectsBalance()) {
            return;
        }
        Account account = findOwnedAccountOrThrow(entry.getAccountId(), entry.getUserId());
        BigDecimal amount = nz(entry.getAmount());
        if (entry.getDirection() == CashDirection.INCOME) {
            account.setBalance(account.getBalance().add(amount));
        } else {
            account.setBalance(account.getBalance().subtract(amount));
        }
        accountRepository.save(account);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void reverseCashEntry(CashEntry entry) {
        if (!entry.isAffectsBalance()) {
            return;
        }
        Account account = findOwnedAccountOrThrow(entry.getAccountId(), entry.getUserId());
        BigDecimal amount = nz(entry.getAmount());
        if (entry.getDirection() == CashDirection.INCOME) {
            account.setBalance(account.getBalance().subtract(amount));
        } else {
            account.setBalance(account.getBalance().add(amount));
        }
        accountRepository.save(account);
    }

    /**
     * Applies balance effect only for settled rows:
     * GIVE decreases, TAKE increases.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void applyLedger(LedgerEntry ledger) {
        if (ledger.getStatus() != LedgerStatus.SETTLED) {
            return;
        }
        if (ledger.getAccountId() == null) {
            throw new IllegalArgumentException(
                    "accountId is required when status is SETTLED so the linked account balance can be updated.");
        }
        Account account = findOwnedAccountOrThrow(ledger.getAccountId(), ledger.getUserId());
        BigDecimal amount = nz(ledger.getAmount()).setScale(2, RoundingMode.HALF_UP);
        if (ledger.getType() == LedgerType.GIVE) {
            account.setBalance(account.getBalance().subtract(amount));
        } else {
            account.setBalance(account.getBalance().add(amount));
        }
        accountRepository.save(account);
    }

    /**
     * Reverses prior settled effect:
     * GIVE reversal adds back, TAKE reversal subtracts.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void reverseLedger(LedgerEntry ledger) {
        if (ledger.getStatus() != LedgerStatus.SETTLED || ledger.getAccountId() == null) {
            return;
        }
        Account account = findOwnedAccountOrThrow(ledger.getAccountId(), ledger.getUserId());
        BigDecimal amount = nz(ledger.getAmount()).setScale(2, RoundingMode.HALF_UP);
        if (ledger.getType() == LedgerType.GIVE) {
            account.setBalance(account.getBalance().add(amount));
        } else {
            account.setBalance(account.getBalance().subtract(amount));
        }
        accountRepository.save(account);
    }

    private Account findOwnedAccountOrThrow(Long accountId, Long userId) {
        Account account = accountRepository
                .findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));
        if (!account.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Account not found with id: " + accountId);
        }
        return account;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

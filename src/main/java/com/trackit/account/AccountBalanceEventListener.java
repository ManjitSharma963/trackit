package com.trackit.account;

import com.trackit.cash.model.CashEntry;
import com.trackit.events.CashEntryCreatedEvent;
import com.trackit.events.LedgerUpdatedEvent;
import com.trackit.ledger.model.LedgerEntry;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AccountBalanceEventListener {

    private final AccountBalanceService accountBalanceService;

    public AccountBalanceEventListener(AccountBalanceService accountBalanceService) {
        this.accountBalanceService = accountBalanceService;
    }

    @EventListener
    public void onCashEntryCreated(CashEntryCreatedEvent event) {
        CashEntry entry = new CashEntry();
        entry.setId(event.entryId());
        entry.setUserId(event.userId());
        entry.setAccountId(event.accountId());
        entry.setDirection(event.direction());
        entry.setAmount(event.amount());
        accountBalanceService.applyCashEntry(entry);
    }

    @EventListener
    public void onLedgerUpdated(LedgerUpdatedEvent event) {
        accountBalanceService.reverseLedger(toLedgerEntry(event.before()));
        accountBalanceService.applyLedger(toLedgerEntry(event.after()));
    }

    private static LedgerEntry toLedgerEntry(LedgerUpdatedEvent.LedgerBalanceSnapshot snap) {
        LedgerEntry e = new LedgerEntry();
        e.setUserId(snap.userId());
        e.setStatus(snap.status());
        e.setAccountId(snap.accountId());
        e.setType(snap.type());
        e.setAmount(snap.amount());
        return e;
    }
}


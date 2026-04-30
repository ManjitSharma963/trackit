package com.trackit.dashboard.service;

import com.trackit.dashboard.dto.LedgerSummaryDto;
import com.trackit.ledger.model.LedgerStatus;
import com.trackit.ledger.repository.LedgerRepository;
import org.springframework.stereotype.Service;

@Service
public class LedgerSummaryService {

    private final LedgerRepository ledgerRepository;

    public LedgerSummaryService(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    public LedgerSummaryDto getLedgerSummary(Long userId) {
        long totalLedger = ledgerRepository.countByUserId(userId);
        long pendingLedger = ledgerRepository.countByUserIdAndStatus(userId, LedgerStatus.PENDING);
        long settledLedger = ledgerRepository.countByUserIdAndStatus(userId, LedgerStatus.SETTLED);
        return new LedgerSummaryDto(totalLedger, pendingLedger, settledLedger);
    }
}


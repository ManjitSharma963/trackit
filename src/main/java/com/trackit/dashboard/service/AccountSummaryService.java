package com.trackit.dashboard.service;

import com.trackit.account.AccountRepository;
import com.trackit.dashboard.dto.AccountsSummaryDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AccountSummaryService {

    private final AccountRepository accountRepository;

    public AccountSummaryService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountsSummaryDto getAccountsSummary(Long userId) {
        long accountCount = accountRepository.countByUserId(userId);
        BigDecimal totalBalance = nz(accountRepository.sumBalanceByUserId(userId));
        return new AccountsSummaryDto(accountCount, totalBalance);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}


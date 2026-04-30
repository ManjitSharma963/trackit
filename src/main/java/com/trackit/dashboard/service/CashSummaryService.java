package com.trackit.dashboard.service;

import com.trackit.cash.dto.CashEntrySummaryResponse;
import com.trackit.cash.model.CashDirection;
import com.trackit.cash.repository.CashEntryRepository;
import com.trackit.dashboard.dto.ExpensesSummaryDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class CashSummaryService {

    private final CashEntryRepository cashEntryRepository;

    public CashSummaryService(CashEntryRepository cashEntryRepository) {
        this.cashEntryRepository = cashEntryRepository;
    }

    public CashSummary getCashAndExpenseSummary(Long userId, LocalDate today) {
        BigDecimal income = nz(cashEntryRepository.sumAmountByUserIdAndDirection(userId, CashDirection.INCOME));
        BigDecimal expense = nz(cashEntryRepository.sumAmountByUserIdAndDirection(userId, CashDirection.EXPENSE));
        CashEntrySummaryResponse cash = new CashEntrySummaryResponse(income, expense, income.subtract(expense));
        ExpensesSummaryDto expenses =
                new ExpensesSummaryDto(expense, cashEntryRepository.countByUserIdAndDirection(userId, CashDirection.EXPENSE));

        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
        BigDecimal incomeMtd =
                nz(cashEntryRepository.sumAmountByUserIdAndDirectionAndEntryDateBetween(
                        userId, CashDirection.INCOME, monthStart, monthEnd));
        BigDecimal expenseMtd =
                nz(cashEntryRepository.sumAmountByUserIdAndDirectionAndEntryDateBetween(
                        userId, CashDirection.EXPENSE, monthStart, monthEnd));
        CashEntrySummaryResponse cashThisMonth =
                new CashEntrySummaryResponse(incomeMtd, expenseMtd, incomeMtd.subtract(expenseMtd));

        return new CashSummary(cash, cashThisMonth, expenses);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    public record CashSummary(
            CashEntrySummaryResponse cash,
            CashEntrySummaryResponse cashThisMonth,
            ExpensesSummaryDto expenses) {}
}


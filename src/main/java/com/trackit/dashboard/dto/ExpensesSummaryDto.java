package com.trackit.dashboard.dto;

import java.math.BigDecimal;

public class ExpensesSummaryDto {

    private BigDecimal totalExpense;
    private long entryCount;

    public ExpensesSummaryDto() {
    }

    public ExpensesSummaryDto(BigDecimal totalExpense, long entryCount) {
        this.totalExpense = totalExpense;
        this.entryCount = entryCount;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(BigDecimal totalExpense) {
        this.totalExpense = totalExpense;
    }

    public long getEntryCount() {
        return entryCount;
    }

    public void setEntryCount(long entryCount) {
        this.entryCount = entryCount;
    }
}

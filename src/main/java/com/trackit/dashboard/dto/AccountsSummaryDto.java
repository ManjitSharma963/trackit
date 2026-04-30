package com.trackit.dashboard.dto;

import java.math.BigDecimal;

public class AccountsSummaryDto {

    private long accountCount;
    private BigDecimal totalBalance;

    public AccountsSummaryDto() {
    }

    public AccountsSummaryDto(long accountCount, BigDecimal totalBalance) {
        this.accountCount = accountCount;
        this.totalBalance = totalBalance;
    }

    public long getAccountCount() {
        return accountCount;
    }

    public void setAccountCount(long accountCount) {
        this.accountCount = accountCount;
    }

    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(BigDecimal totalBalance) {
        this.totalBalance = totalBalance;
    }
}

package com.trackit.dashboard.dto;

public class LedgerSummaryDto {

    private long totalEntries;
    private long pendingEntries;
    private long settledEntries;

    public LedgerSummaryDto() {
    }

    public LedgerSummaryDto(long totalEntries, long pendingEntries, long settledEntries) {
        this.totalEntries = totalEntries;
        this.pendingEntries = pendingEntries;
        this.settledEntries = settledEntries;
    }

    public long getTotalEntries() {
        return totalEntries;
    }

    public void setTotalEntries(long totalEntries) {
        this.totalEntries = totalEntries;
    }

    public long getPendingEntries() {
        return pendingEntries;
    }

    public void setPendingEntries(long pendingEntries) {
        this.pendingEntries = pendingEntries;
    }

    public long getSettledEntries() {
        return settledEntries;
    }

    public void setSettledEntries(long settledEntries) {
        this.settledEntries = settledEntries;
    }
}

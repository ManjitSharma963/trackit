package com.trackit.dashboard.dto;

public class ScheduleSummaryDto {

    private long todayTotal;
    private long todayPending;
    private long todayDone;
    /** Pending items dated within the next seven days from today (inclusive). */
    private long pendingNextSevenDays;
    /** Pending items dated strictly before today. */
    private long overduePending;

    public ScheduleSummaryDto() {
    }

    public ScheduleSummaryDto(
            long todayTotal,
            long todayPending,
            long todayDone,
            long pendingNextSevenDays,
            long overduePending) {
        this.todayTotal = todayTotal;
        this.todayPending = todayPending;
        this.todayDone = todayDone;
        this.pendingNextSevenDays = pendingNextSevenDays;
        this.overduePending = overduePending;
    }

    public long getTodayTotal() {
        return todayTotal;
    }

    public void setTodayTotal(long todayTotal) {
        this.todayTotal = todayTotal;
    }

    public long getTodayPending() {
        return todayPending;
    }

    public void setTodayPending(long todayPending) {
        this.todayPending = todayPending;
    }

    public long getTodayDone() {
        return todayDone;
    }

    public void setTodayDone(long todayDone) {
        this.todayDone = todayDone;
    }

    public long getPendingNextSevenDays() {
        return pendingNextSevenDays;
    }

    public void setPendingNextSevenDays(long pendingNextSevenDays) {
        this.pendingNextSevenDays = pendingNextSevenDays;
    }

    public long getOverduePending() {
        return overduePending;
    }

    public void setOverduePending(long overduePending) {
        this.overduePending = overduePending;
    }
}

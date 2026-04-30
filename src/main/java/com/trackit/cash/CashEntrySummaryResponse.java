package com.trackit.cash.dto;

import com.trackit.cash.model.*;

import java.math.BigDecimal;

public class CashEntrySummaryResponse {

    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal net;

    public CashEntrySummaryResponse() {
    }

    public CashEntrySummaryResponse(BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal net) {
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.net = net;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(BigDecimal totalExpense) {
        this.totalExpense = totalExpense;
    }

    public BigDecimal getNet() {
        return net;
    }

    public void setNet(BigDecimal net) {
        this.net = net;
    }
}

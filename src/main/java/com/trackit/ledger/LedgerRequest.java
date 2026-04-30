package com.trackit.ledger.dto;

import com.trackit.ledger.model.*;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LedgerRequest {

    @NotBlank
    private String personName;

    @NotNull
    private LedgerType type;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    private String notes;

    @NotNull
    private LocalDate transactionDate;

    private LedgerStatus status;

    /**
     * Required when {@code status} is SETTLED unless token contains {@code accountId}, in which case that value is used.
     * Optional while PENDING.
     */
    private Long accountId;

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public LedgerType getType() {
        return type;
    }

    public void setType(LedgerType type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public LedgerStatus getStatus() {
        return status;
    }

    public void setStatus(LedgerStatus status) {
        this.status = status;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }
}

package com.trackit.cash.dto;

import com.trackit.cash.model.*;

import com.trackit.common.RecurrenceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CashEntryRequest {

    private Long userId;

    /**
     * When omitted, the server picks this user's default wallet (same rule as newest account in
     * {@code GET /api/accounts}) using only the JWT identity — no account id is stored in the token.
     */
    @Nullable
    @Schema(
            description =
                    "Optional. Omit to use your default account (newest by id for this user). JWT identifies the user only.")
    private Long accountId;

    @NotNull
    private CashDirection direction;

    @Size(max = 255)
    private String title;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @Size(max = 100)
    private String category;

    @Size(max = 2000)
    private String notes;

    @NotNull
    private LocalDate entryDate;

    private boolean recurring;

    private RecurrenceType recurrenceType;

    @Schema(
            description =
                    "For EXPENSE: optional budget line id (`budget_categories.id`) on the active plan; omit for label-only spend. Must not be set for INCOME.",
            example = "1")
    private Long categoryId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public CashDirection getDirection() {
        return direction;
    }

    public void setDirection(CashDirection direction) {
        this.direction = direction;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public boolean isRecurring() {
        return recurring;
    }

    public void setRecurring(boolean recurring) {
        this.recurring = recurring;
    }

    public RecurrenceType getRecurrenceType() {
        return recurrenceType;
    }

    public void setRecurrenceType(RecurrenceType recurrenceType) {
        this.recurrenceType = recurrenceType;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
}

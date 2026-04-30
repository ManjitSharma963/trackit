package com.trackit.cash.controller;

import com.trackit.cash.dto.*;
import com.trackit.cash.model.*;
import com.trackit.cash.service.*;
import com.trackit.auth.service.CurrentUserProvider;
import com.trackit.common.PageResponse;
import com.trackit.idempotency.IdempotencyKeyFilter;
import com.trackit.idempotency.IdempotencyService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cash-entries")
@PreAuthorize("hasRole('USER')")
public class CashEntryController {

    private final CashEntryService cashEntryService;
    private final CurrentUserProvider currentUserProvider;
    private final IdempotencyService idempotencyService;

    public CashEntryController(
            CashEntryService cashEntryService,
            CurrentUserProvider currentUserProvider,
            IdempotencyService idempotencyService) {
        this.cashEntryService = cashEntryService;
        this.currentUserProvider = currentUserProvider;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping
    @Operation(
            summary = "Create cash entry",
            description =
                    "JWT identifies the user only (subject = user id). Omit `accountId` to use that user's default account (newest account row). "
                            + "For EXPENSE, `categoryId` links to an active-plan budget line when set; omit for label-only expenses. INCOME must omit `categoryId`. "
                            + "Optional `category` is a free-text label.")
    public ResponseEntity<CashEntryResponse> create(
            @Valid @RequestBody CashEntryRequest request,
            HttpServletRequest httpRequest) {
        return runIdempotent(httpRequest, HttpStatus.CREATED, () -> cashEntryService.create(request));
    }

    @GetMapping("/summary")
    public CashEntrySummaryResponse summary() {
        return cashEntryService.summarize();
    }

    @GetMapping
    public PageResponse<CashEntryResponse> list(
            @RequestParam(required = false) CashDirection direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return cashEntryService.list(direction, page, size);
    }

    @GetMapping(params = {"from", "to", "type"})
    public List<CashEntryResponse> listExpensesByDateRange(
            @RequestParam("from")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam("to")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @RequestParam CashDirection type) {
        if (type != CashDirection.EXPENSE) {
            throw new IllegalArgumentException("Only type=EXPENSE is supported by this endpoint.");
        }
        return cashEntryService.listByDateRange(type, from, to);
    }

    @GetMapping("/{id}")
    public CashEntryResponse getById(@PathVariable Long id) {
        return cashEntryService.getById(id);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update cash entry",
            description =
                    "Omit `accountId` to keep the entry's current account. Same `categoryId` rules as create.")
    public ResponseEntity<CashEntryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CashEntryRequest request,
            HttpServletRequest httpRequest) {
        return runIdempotent(httpRequest, HttpStatus.OK, () -> cashEntryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        cashEntryService.delete(id);
    }

    private ResponseEntity<CashEntryResponse> runIdempotent(
            HttpServletRequest httpRequest,
            HttpStatus freshStatus,
            java.util.function.Supplier<CashEntryResponse> action) {
        String key = (String) httpRequest.getAttribute(IdempotencyKeyFilter.IDEMPOTENCY_ATTR);
        if (key == null || key.isBlank()) {
            return ResponseEntity.status(freshStatus).body(action.get());
        }

        Long userId = currentUserProvider.getCurrentUserId();
        String method = httpRequest.getMethod();
        String path = httpRequest.getRequestURI();
        IdempotencyService.AcquireResult<CashEntryResponse> acquire =
                idempotencyService.acquire(userId, key, method, path, CashEntryResponse.class);

        if (acquire.state() == IdempotencyService.AcquireState.REPLAY) {
            return ResponseEntity.status(acquire.replay().statusCode()).body(acquire.replay().body());
        }
        if (acquire.state() == IdempotencyService.AcquireState.IN_PROGRESS) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        Long rowId = acquire.rowId();
        try {
            CashEntryResponse body = action.get();
            idempotencyService.markCompleted(rowId, freshStatus.value(), body);
            return ResponseEntity.status(freshStatus).body(body);
        } catch (RuntimeException ex) {
            idempotencyService.releaseOnFailure(rowId);
            throw ex;
        }
    }
}

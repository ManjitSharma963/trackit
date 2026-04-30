package com.trackit.ledger.controller;

import com.trackit.ledger.dto.*;
import com.trackit.ledger.service.*;

import com.trackit.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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

@RestController
@RequestMapping("/api/v1/ledger")
@PreAuthorize("hasRole('USER')")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create ledger entry",
            description =
                    "When `status` is SETTLED, `accountId` must be set: GIVE decreases that account balance, TAKE increases it. PENDING rows may omit `accountId` until you settle.")
    public LedgerResponse create(@Valid @RequestBody LedgerRequest request) {
        return ledgerService.create(request);
    }

    @GetMapping
    public PageResponse<LedgerResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ledgerService.list(page, size);
    }

    @GetMapping("/{id}")
    public LedgerResponse getById(@PathVariable Long id) {
        return ledgerService.getById(id);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update ledger entry",
            description =
                    "Changing to SETTLED applies the balance movement once; changing away from SETTLED reverses it. Editing a settled row (amount/type/account) re-applies the net effect.")
    public LedgerResponse update(
            @PathVariable Long id,
            @Valid @RequestBody LedgerRequest request) {
        return ledgerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        ledgerService.delete(id);
    }
}

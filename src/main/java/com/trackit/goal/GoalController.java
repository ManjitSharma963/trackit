package com.trackit.goals.controller;

import com.trackit.goals.dto.CreateGoalRequest;
import com.trackit.goals.dto.GoalResponse;
import com.trackit.goals.dto.UpdateGoalProgressRequest;
import com.trackit.goals.service.GoalService;

import com.trackit.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/goals")
@PreAuthorize("hasRole('USER')")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create goal")
    public GoalResponse create(@Valid @RequestBody CreateGoalRequest request) {
        return goalService.create(request);
    }

    @PutMapping("/{id}/progress")
    @Operation(summary = "Update goal progress", description = "Sets currentAmount to the given absolute value.")
    public GoalResponse updateProgress(@PathVariable Long id, @Valid @RequestBody UpdateGoalProgressRequest request) {
        return goalService.updateProgress(id, request);
    }

    @GetMapping
    @Operation(summary = "List goals")
    public PageResponse<GoalResponse> list(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return goalService.list(page, size);
    }
}

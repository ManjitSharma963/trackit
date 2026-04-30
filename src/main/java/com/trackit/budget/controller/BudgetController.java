package com.trackit.budget.controller;

import com.trackit.budget.dto.BudgetCategoryResponse;
import com.trackit.budget.dto.BudgetPlanResponse;
import com.trackit.budget.dto.BudgetStatusResponse;
import com.trackit.budget.dto.CreateCategoryRequest;
import com.trackit.budget.dto.CreatePlanRequest;
import com.trackit.budget.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/budget")
@PreAuthorize("hasRole('USER')")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping("/plans")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create budget plan", description = "JWT user owns the plan. Optional `isActive` deactivates other plans for this user.")
    public BudgetPlanResponse createPlan(@Valid @RequestBody CreatePlanRequest request) {
        return budgetService.createPlan(request);
    }

    @GetMapping("/plans")
    public List<BudgetPlanResponse> listPlans() {
        return budgetService.listPlans();
    }

    @PatchMapping("/plans/{id}/activate")
    public BudgetPlanResponse activatePlan(@PathVariable("id") Long planId) {
        return budgetService.activatePlan(planId);
    }

    @PatchMapping("/plans/{id}/deactivate")
    public BudgetPlanResponse deactivatePlan(@PathVariable("id") Long planId) {
        return budgetService.deactivatePlan(planId);
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add budget category to a plan", description = "Plan must belong to the JWT user. Percentage plans enforce total % ≤ 100.")
    public BudgetCategoryResponse addCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return budgetService.addCategory(request);
    }

    @GetMapping("/categories")
    public List<BudgetCategoryResponse> listCategories(@RequestParam(value = "planId", required = false) Long planId) {
        return budgetService.listCategories(planId);
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable("id") Long categoryId) {
        budgetService.deleteCategory(categoryId);
    }

    @GetMapping("/status")
    @Operation(
            summary = "Get budget status (active plan)",
            description =
                    "Requires an active plan. Per category: allocated = percentage ? totalIncome×value/100 : value; spent from EXPENSE cash entries with matching categoryId; remaining = allocated − spent.")
    public BudgetStatusResponse status() {
        return budgetService.getStatus();
    }
}

package com.trackit.budget.service;

import com.trackit.auth.service.CurrentUserProvider;
import com.trackit.budget.dto.BudgetCategoryResponse;
import com.trackit.budget.dto.BudgetCategoryStatusDto;
import com.trackit.budget.dto.BudgetPlanResponse;
import com.trackit.budget.dto.BudgetStatusResponse;
import com.trackit.budget.dto.DashboardBudgetPayloadDto;
import com.trackit.budget.dto.DashboardBudgetSummaryDto;
import com.trackit.budget.dto.DashboardOverBudgetCategoryDto;
import com.trackit.budget.dto.CreateCategoryRequest;
import com.trackit.budget.dto.CreatePlanRequest;
import com.trackit.budget.model.BudgetCategory;
import com.trackit.budget.model.BudgetPlan;
import com.trackit.budget.repository.BudgetCategoryRepository;
import com.trackit.budget.repository.BudgetPlanRepository;
import com.trackit.cash.model.CashDirection;
import com.trackit.cash.repository.CashEntryRepository;
import com.trackit.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final BudgetPlanRepository budgetPlanRepository;
    private final BudgetCategoryRepository budgetCategoryRepository;
    private final CashEntryRepository cashEntryRepository;
    private final CurrentUserProvider currentUserProvider;

    public BudgetService(
            BudgetPlanRepository budgetPlanRepository,
            BudgetCategoryRepository budgetCategoryRepository,
            CashEntryRepository cashEntryRepository,
            CurrentUserProvider currentUserProvider) {
        this.budgetPlanRepository = budgetPlanRepository;
        this.budgetCategoryRepository = budgetCategoryRepository;
        this.cashEntryRepository = cashEntryRepository;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * EXPENSE cash entries require {@code categoryId} (budget line). The category must exist, belong to the JWT
     * user, and sit under that user's <strong>active</strong> budget plan. INCOME entries must not send {@code categoryId}.
     */
    public void validateCashEntryBudgetLink(Long userId, CashDirection direction, Long categoryId) {
        if (direction == CashDirection.EXPENSE) {
            // Backward-compatible path for lightweight expense capture screens:
            // if categoryId is omitted, we allow the entry and treat free-text `category` as label-only.
            if (categoryId == null) {
                return;
            }
            assertExpenseCategoryAllowed(userId, categoryId);
            return;
        }
        if (categoryId != null) {
            throw new IllegalArgumentException("categoryId is only allowed for EXPENSE cash entries.");
        }
    }

    @Transactional
    public BudgetPlanResponse createPlan(CreatePlanRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        if (request.isActive()) {
            budgetPlanRepository.deactivateAllForUser(userId);
        }
        BudgetPlan plan = new BudgetPlan();
        plan.setUserId(userId);
        plan.setName(request.getName().trim());
        plan.setTotalIncome(request.getTotalIncome());
        plan.setPercentage(Boolean.TRUE.equals(request.getPercentage()));
        plan.setActive(request.isActive());
        return toPlanResponse(budgetPlanRepository.save(plan));
    }

    public List<BudgetPlanResponse> listPlans() {
        Long userId = currentUserProvider.getCurrentUserId();
        return budgetPlanRepository.findByUserIdOrderByIdDesc(userId).stream()
                .map(this::toPlanResponse)
                .toList();
    }

    @Transactional
    public BudgetPlanResponse activatePlan(Long planId) {
        Long userId = currentUserProvider.getCurrentUserId();
        BudgetPlan plan = findOwnedPlanOrThrow(userId, planId);
        budgetPlanRepository.deactivateAllForUser(userId);
        plan.setActive(true);
        return toPlanResponse(budgetPlanRepository.save(plan));
    }

    @Transactional
    public BudgetPlanResponse deactivatePlan(Long planId) {
        Long userId = currentUserProvider.getCurrentUserId();
        BudgetPlan plan = findOwnedPlanOrThrow(userId, planId);
        plan.setActive(false);
        return toPlanResponse(budgetPlanRepository.save(plan));
    }

    @Transactional
    public BudgetCategoryResponse addCategory(CreateCategoryRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        BudgetPlan plan = findOwnedPlanOrThrow(userId, request.getPlanId());
        validateNewCategoryValue(plan, request.getValue());
        BudgetCategory row = new BudgetCategory();
        row.setPlan(plan);
        row.setName(request.getName().trim());
        row.setType(request.getType());
        row.setValue(request.getValue());
        return toCategoryResponse(budgetCategoryRepository.save(row));
    }

    public List<BudgetCategoryResponse> listCategories(Long planId) {
        Long userId = currentUserProvider.getCurrentUserId();
        Long resolvedPlanId = planId;
        if (resolvedPlanId == null) {
            Optional<BudgetPlan> active = budgetPlanRepository.findByUserIdAndActiveTrue(userId);
            if (active.isEmpty()) {
                // Mobile clients call GET /categories without planId; 200 + [] avoids noisy 400 when no plan yet.
                return List.of();
            }
            resolvedPlanId = active.get().getId();
        }
        findOwnedPlanOrThrow(userId, resolvedPlanId);
        return budgetCategoryRepository.findByPlan_IdOrderByIdAsc(resolvedPlanId).stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        Long userId = currentUserProvider.getCurrentUserId();
        BudgetCategory category = budgetCategoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget category not found with id: " + categoryId));
        BudgetPlan plan = category.getPlan();
        if (!plan.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Budget category not found with id: " + categoryId);
        }
        if (cashEntryRepository.countByCategoryId(categoryId) > 0) {
            throw new IllegalArgumentException(
                    "Cannot delete budget category " + categoryId + " while cash entries still reference it.");
        }
        budgetCategoryRepository.delete(category);
    }

    public BudgetStatusResponse getStatus() {
        Long userId = currentUserProvider.getCurrentUserId();
        BudgetPlan active = budgetPlanRepository
                .findByUserIdAndActiveTrue(userId)
                .orElseThrow(() -> new IllegalArgumentException("No active budget plan. Create one and activate it, or activate an existing plan."));
        return buildBudgetStatusResponse(active);
    }

    /**
     * Active-plan dashboard bundle: full {@link BudgetStatusResponse}, MTD {@link DashboardBudgetSummaryDto}
     * (including today's suggested spending cap), and categories over their monthly allocation this month.
     */
    public Optional<DashboardBudgetPayloadDto> getDashboardBudgetPayload() {
        Long userId = currentUserProvider.getCurrentUserId();
        return budgetPlanRepository.findByUserIdAndActiveTrue(userId).map(plan -> buildDashboardBudgetPayload(plan, userId));
    }

    private BudgetStatusResponse buildBudgetStatusResponse(BudgetPlan active) {
        Long userId = active.getUserId();
        List<BudgetCategory> categories = budgetCategoryRepository.findByPlan_IdOrderByIdAsc(active.getId());
        Set<Long> ids = categories.stream().map(BudgetCategory::getId).collect(Collectors.toSet());
        Map<Long, BigDecimal> spentByCategory = loadExpenseTotalsByCategory(userId, ids);

        List<BudgetCategoryStatusDto> rows = new ArrayList<>();
        for (BudgetCategory cat : categories) {
            BigDecimal allocated = allocatedAmount(active, cat.getValue());
            BigDecimal spent = spentByCategory.getOrDefault(cat.getId(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            BudgetCategoryStatusDto dto = new BudgetCategoryStatusDto();
            dto.setCategoryId(cat.getId());
            dto.setName(cat.getName());
            dto.setType(cat.getType());
            dto.setAllocatedAmount(allocated);
            dto.setSpentAmount(spent);
            dto.setRemainingAmount(allocated.subtract(spent));
            rows.add(dto);
        }
        rows.sort(Comparator.comparing(BudgetCategoryStatusDto::getSpentAmount)
                .reversed()
                .thenComparing(BudgetCategoryStatusDto::getCategoryId));

        BudgetStatusResponse response = new BudgetStatusResponse();
        response.setPlanId(active.getId());
        response.setPlanName(active.getName());
        response.setTotalIncome(active.getTotalIncome());
        response.setCategories(rows);
        return response;
    }

    private DashboardBudgetPayloadDto buildDashboardBudgetPayload(BudgetPlan plan, Long userId) {
        BudgetStatusResponse status = buildBudgetStatusResponse(plan);
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        List<BudgetCategory> categories = budgetCategoryRepository.findByPlan_IdOrderByIdAsc(plan.getId());
        Set<Long> ids = categories.stream().map(BudgetCategory::getId).collect(Collectors.toSet());
        Map<Long, BigDecimal> spentMtd =
                loadExpenseTotalsByCategoryInDateRange(userId, ids, monthStart, monthEnd);

        BigDecimal totalAllocated = BigDecimal.ZERO;
        BigDecimal totalSpentMtd = BigDecimal.ZERO;
        List<DashboardOverBudgetCategoryDto> over = new ArrayList<>();

        for (BudgetCategory cat : categories) {
            BigDecimal allocated = allocatedAmount(plan, cat.getValue());
            BigDecimal spent =
                    spentMtd.getOrDefault(cat.getId(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            totalAllocated = totalAllocated.add(allocated);
            totalSpentMtd = totalSpentMtd.add(spent);
            if (spent.compareTo(allocated) > 0) {
                DashboardOverBudgetCategoryDto row = new DashboardOverBudgetCategoryDto();
                row.setCategoryId(cat.getId());
                row.setName(cat.getName());
                row.setType(cat.getType());
                row.setAllocatedAmount(allocated);
                row.setSpentThisMonth(spent);
                row.setOverAmount(spent.subtract(allocated));
                over.add(row);
            }
        }
        over.sort(Comparator.comparing(DashboardOverBudgetCategoryDto::getOverAmount).reversed());

        BigDecimal remaining = totalAllocated.subtract(totalSpentMtd).setScale(2, RoundingMode.HALF_UP);
        int daysRemaining = (int) ChronoUnit.DAYS.between(today, monthEnd) + 1;
        BigDecimal todayLimit = BigDecimal.ZERO;
        if (remaining.compareTo(BigDecimal.ZERO) > 0 && daysRemaining > 0) {
            todayLimit = remaining.divide(BigDecimal.valueOf(daysRemaining), 2, RoundingMode.HALF_UP);
        }

        BigDecimal utilization = null;
        if (totalAllocated.compareTo(BigDecimal.ZERO) > 0) {
            utilization =
                    totalSpentMtd.multiply(HUNDRED).divide(totalAllocated, 2, RoundingMode.HALF_UP);
        }

        DashboardBudgetSummaryDto summary = new DashboardBudgetSummaryDto();
        summary.setPlanId(plan.getId());
        summary.setPlanName(plan.getName());
        summary.setTotalIncome(plan.getTotalIncome());
        summary.setTotalAllocated(totalAllocated.setScale(2, RoundingMode.HALF_UP));
        summary.setTotalSpentThisMonth(totalSpentMtd.setScale(2, RoundingMode.HALF_UP));
        summary.setRemainingThisMonth(remaining);
        summary.setUtilizationPercent(utilization);
        summary.setDaysRemainingInMonth(daysRemaining);
        summary.setTodaySpendingLimit(todayLimit);

        DashboardBudgetPayloadDto payload = new DashboardBudgetPayloadDto();
        payload.setStatus(status);
        payload.setSummary(summary);
        payload.setOverBudgetCategories(over);
        return payload;
    }

    private Map<Long, BigDecimal> loadExpenseTotalsByCategoryInDateRange(
            Long userId, Set<Long> categoryIds, LocalDate fromInclusive, LocalDate toInclusive) {
        Map<Long, BigDecimal> map = new HashMap<>();
        if (categoryIds.isEmpty()) {
            return map;
        }
        List<Object[]> tuples =
                cashEntryRepository.sumExpensesGroupedByCategoryIdsAndEntryDateBetween(
                        userId, CashDirection.EXPENSE, categoryIds, fromInclusive, toInclusive);
        for (Object[] row : tuples) {
            if (row[0] == null) {
                continue;
            }
            Long cid = ((Number) row[0]).longValue();
            BigDecimal sum = row[1] instanceof BigDecimal b ? b : BigDecimal.valueOf(((Number) row[1]).doubleValue());
            map.put(cid, sum != null ? sum : BigDecimal.ZERO);
        }
        return map;
    }

    private Map<Long, BigDecimal> loadExpenseTotalsByCategory(Long userId, Set<Long> categoryIds) {
        Map<Long, BigDecimal> map = new HashMap<>();
        if (categoryIds.isEmpty()) {
            return map;
        }
        List<Object[]> tuples =
                cashEntryRepository.sumExpensesGroupedByCategoryIds(userId, CashDirection.EXPENSE, categoryIds);
        for (Object[] row : tuples) {
            if (row[0] == null) {
                continue;
            }
            Long cid = ((Number) row[0]).longValue();
            BigDecimal sum = row[1] instanceof BigDecimal b ? b : BigDecimal.valueOf(((Number) row[1]).doubleValue());
            map.put(cid, sum != null ? sum : BigDecimal.ZERO);
        }
        return map;
    }

    private void assertExpenseCategoryAllowed(Long userId, Long categoryId) {
        BudgetCategory category = budgetCategoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget category not found with id: " + categoryId));
        BudgetPlan plan = category.getPlan();
        if (!Objects.equals(plan.getUserId(), userId)) {
            throw new ResourceNotFoundException("Budget category not found with id: " + categoryId);
        }
        if (!plan.isActive()) {
            throw new IllegalArgumentException(
                    "Budget category belongs to an inactive plan. Activate that plan first, or choose a category from the active plan.");
        }
    }

    private BudgetPlan findOwnedPlanOrThrow(Long userId, Long planId) {
        return budgetPlanRepository
                .findById(planId)
                .filter(p -> Objects.equals(p.getUserId(), userId))
                .orElseThrow(() -> new ResourceNotFoundException("Budget plan not found with id: " + planId));
    }

    private void validateNewCategoryValue(BudgetPlan plan, BigDecimal newValue) {
        if (plan.isPercentage()) {
            if (newValue.compareTo(BigDecimal.ZERO) <= 0 || newValue.compareTo(HUNDRED) > 0) {
                throw new IllegalArgumentException("Percentage category value must be between 0.01 and 100.");
            }
            BigDecimal sum = budgetCategoryRepository.sumValuesByPlanId(plan.getId());
            BigDecimal after = sum.add(newValue);
            if (after.compareTo(HUNDRED) > 0) {
                throw new IllegalArgumentException(
                        "Total percentage allocation for this plan would exceed 100% (currently "
                                + sum.stripTrailingZeros().toPlainString()
                                + "% plus "
                                + newValue.stripTrailingZeros().toPlainString()
                                + "%).");
            }
        } else {
            if (newValue.compareTo(new BigDecimal("0.01")) < 0) {
                throw new IllegalArgumentException("Fixed category allocation must be at least 0.01.");
            }
        }
    }

    /**
     * allocated = plan.isPercentage()
     *     ? plan.getTotalIncome() * categoryValue / 100
     *     : categoryValue;
     * (Monetary values rounded to 2 decimal places.)
     */
    private BigDecimal allocatedAmount(BudgetPlan plan, BigDecimal categoryValue) {
        if (plan.isPercentage()) {
            return plan.getTotalIncome()
                    .multiply(categoryValue)
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        }
        return categoryValue.setScale(2, RoundingMode.HALF_UP);
    }

    private BudgetPlanResponse toPlanResponse(BudgetPlan plan) {
        BudgetPlanResponse r = new BudgetPlanResponse();
        r.setId(plan.getId());
        r.setName(plan.getName());
        r.setTotalIncome(plan.getTotalIncome());
        r.setPercentage(plan.isPercentage());
        r.setActive(plan.isActive());
        r.setCreatedAt(plan.getCreatedAt());
        return r;
    }

    private BudgetCategoryResponse toCategoryResponse(BudgetCategory c) {
        BudgetCategoryResponse r = new BudgetCategoryResponse();
        r.setId(c.getId());
        r.setPlanId(c.getPlan() != null ? c.getPlan().getId() : null);
        r.setName(c.getName());
        r.setType(c.getType());
        r.setValue(c.getValue());
        r.setCreatedAt(c.getCreatedAt());
        return r;
    }
}

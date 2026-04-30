package com.trackit.budget.repository;

import com.trackit.budget.model.BudgetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface BudgetCategoryRepository extends JpaRepository<BudgetCategory, Long> {

    List<BudgetCategory> findByPlan_IdOrderByIdAsc(Long planId);

    @Query("select coalesce(sum(c.value), 0) from BudgetCategory c where c.plan.id = :planId")
    BigDecimal sumValuesByPlanId(@Param("planId") Long planId);
}

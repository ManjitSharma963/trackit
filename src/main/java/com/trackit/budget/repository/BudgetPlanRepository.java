package com.trackit.budget.repository;

import com.trackit.budget.model.BudgetPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BudgetPlanRepository extends JpaRepository<BudgetPlan, Long> {

    List<BudgetPlan> findByUserIdOrderByIdDesc(Long userId);

    Optional<BudgetPlan> findByUserIdAndActiveTrue(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update BudgetPlan p set p.active = false where p.userId = :userId")
    void deactivateAllForUser(@Param("userId") Long userId);
}

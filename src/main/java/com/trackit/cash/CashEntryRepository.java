package com.trackit.cash.repository;

import com.trackit.cash.model.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface CashEntryRepository extends JpaRepository<CashEntry, Long> {

    Page<CashEntry> findByUserId(Long userId, Pageable pageable);

    Page<CashEntry> findByUserIdAndDirection(Long userId, CashDirection direction, Pageable pageable);

    List<CashEntry> findByRecurringTrueAndEntryDate(LocalDate entryDate);

    boolean existsByUserIdAndAccountIdAndDirectionAndTitleAndAmountAndEntryDate(
            Long userId, Long accountId, CashDirection direction, String title, BigDecimal amount, LocalDate entryDate);

    @Query("select coalesce(sum(e.amount), 0) from CashEntry e where e.userId = :userId and e.direction = :direction")
    BigDecimal sumAmountByUserIdAndDirection(@Param("userId") Long userId, @Param("direction") CashDirection direction);

    @Query(
            """
            select coalesce(sum(e.amount), 0) from CashEntry e
            where e.userId = :userId and e.direction = :direction
              and e.entryDate >= :fromInclusive and e.entryDate <= :toInclusive
            """)
    BigDecimal sumAmountByUserIdAndDirectionAndEntryDateBetween(
            @Param("userId") Long userId,
            @Param("direction") CashDirection direction,
            @Param("fromInclusive") LocalDate fromInclusive,
            @Param("toInclusive") LocalDate toInclusive);

    long countByUserIdAndDirection(Long userId, CashDirection direction);

    long countByCategoryId(Long categoryId);

    @Query(
            """
            select e.categoryId, sum(e.amount)
            from CashEntry e
            where e.userId = :userId
              and e.direction = :direction
              and e.categoryId in :categoryIds
            group by e.categoryId
            """)
    List<Object[]> sumExpensesGroupedByCategoryIds(
            @Param("userId") Long userId,
            @Param("direction") CashDirection direction,
            @Param("categoryIds") Collection<Long> categoryIds);

    @Query(
            """
            select e.categoryId, sum(e.amount)
            from CashEntry e
            where e.userId = :userId
              and e.direction = :direction
              and e.categoryId in :categoryIds
              and e.entryDate >= :fromInclusive
              and e.entryDate <= :toInclusive
            group by e.categoryId
            """)
    List<Object[]> sumExpensesGroupedByCategoryIdsAndEntryDateBetween(
            @Param("userId") Long userId,
            @Param("direction") CashDirection direction,
            @Param("categoryIds") Collection<Long> categoryIds,
            @Param("fromInclusive") LocalDate fromInclusive,
            @Param("toInclusive") LocalDate toInclusive);

    @Query(
            """
            select e
            from CashEntry e
            where e.userId = :userId
              and e.direction = :direction
              and e.entryDate >= :fromInclusive
              and e.entryDate <= :toInclusive
            order by e.entryDate desc, e.id desc
            """)
    List<CashEntry> findByUserIdAndDirectionAndEntryDateBetweenOrderByEntryDateDescIdDesc(
            @Param("userId") Long userId,
            @Param("direction") CashDirection direction,
            @Param("fromInclusive") LocalDate fromInclusive,
            @Param("toInclusive") LocalDate toInclusive);
}

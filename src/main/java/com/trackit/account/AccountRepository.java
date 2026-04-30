package com.trackit.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUserIdOrderByIdDesc(Long userId);

    long countByUserId(Long userId);

    @Query("select coalesce(sum(a.balance), 0) from Account a where a.userId = :userId")
    BigDecimal sumBalanceByUserId(@Param("userId") Long userId);
}

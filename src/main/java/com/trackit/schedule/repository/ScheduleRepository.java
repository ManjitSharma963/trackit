package com.trackit.schedule.repository;

import com.trackit.schedule.model.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @Query(
            """
            select s
            from Schedule s
            where s.userId = :userId and s.scheduleDate = :date
            order by
                case when s.scheduleTime is null then 1 else 0 end,
                s.scheduleTime asc,
                s.id asc
            """)
    Page<Schedule> findByUserIdAndDateOrdered(
            @Param("userId") Long userId, @Param("date") LocalDate date, Pageable pageable);

    long countByUserIdAndScheduleDate(Long userId, LocalDate scheduleDate);

    long countByUserIdAndScheduleDateAndStatus(Long userId, LocalDate scheduleDate, ScheduleStatus status);

    long countByUserIdAndScheduleDateBetweenAndStatus(
            Long userId, LocalDate scheduleDateAfter, LocalDate scheduleDateBefore, ScheduleStatus status);

    long countByUserIdAndScheduleDateLessThanAndStatus(Long userId, LocalDate scheduleDate, ScheduleStatus status);

    List<Schedule>
            findByUserIdIsNotNullAndStatusAndReminderMinutesGreaterThanAndScheduleTimeIsNotNullAndReminderSentAtIsNullAndScheduleDateBetween(
                    ScheduleStatus status,
                    int reminderMinutesAfter,
                    LocalDate fromInclusive,
                    LocalDate toInclusive);
}

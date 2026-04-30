package com.trackit.schedule.service;

import com.trackit.schedule.model.Schedule;
import com.trackit.schedule.model.ScheduleStatus;
import com.trackit.schedule.repository.ScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScheduleReminderService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleReminderService.class);

    private final ScheduleRepository scheduleRepository;

    public ScheduleReminderService(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    /**
     * Fire once per schedule when {@code now} is in
     * {@code [eventStart - reminderMinutes, eventStart)} and {@code schedule_time} is set.
     */
    @Transactional
    public int processDueReminders(LocalDateTime now) {
        LocalDate from = now.toLocalDate().minusDays(1);
        LocalDate to = now.toLocalDate().plusDays(1);
        List<Schedule> candidates = scheduleRepository
                .findByUserIdIsNotNullAndStatusAndReminderMinutesGreaterThanAndScheduleTimeIsNotNullAndReminderSentAtIsNullAndScheduleDateBetween(
                        ScheduleStatus.PENDING, 0, from, to);
        int fired = 0;
        for (Schedule schedule : candidates) {
            LocalDateTime eventStart = LocalDateTime.of(schedule.getScheduleDate(), schedule.getScheduleTime());
            LocalDateTime remindAt = eventStart.minusMinutes(schedule.getReminderMinutes());
            if (!now.isBefore(remindAt) && now.isBefore(eventStart)) {
                log.info(
                        "SCHEDULE_REMINDER scheduleId={} title=\"{}\" eventStart={} reminderMinutes={}",
                        schedule.getId(),
                        schedule.getTitle(),
                        eventStart,
                        schedule.getReminderMinutes());
                schedule.setReminderSentAt(now);
                scheduleRepository.save(schedule);
                fired++;
            }
        }
        return fired;
    }
}

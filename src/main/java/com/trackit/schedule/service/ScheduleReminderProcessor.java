package com.trackit.schedule.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(name = "app.scheduler.reminders-enabled", havingValue = "true", matchIfMissing = true)
public class ScheduleReminderProcessor {

    private final ScheduleReminderService scheduleReminderService;

    public ScheduleReminderProcessor(ScheduleReminderService scheduleReminderService) {
        this.scheduleReminderService = scheduleReminderService;
    }

    @Scheduled(cron = "${app.scheduler.reminder-cron:0 * * * * *}")
    public void dispatchScheduleReminders() {
        scheduleReminderService.processDueReminders(LocalDateTime.now());
    }
}

package com.trackit.schedule.dto;

import com.trackit.schedule.model.*;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public class ScheduleRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotNull
    private ScheduleType type;

    @NotNull
    private LocalDate scheduleDate;

    private LocalTime scheduleTime;

    @Size(max = 2000)
    private String notes;

    @Min(0)
    private Integer reminderMinutes;

    private ScheduleRepeatType repeatType;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ScheduleType getType() {
        return type;
    }

    public void setType(ScheduleType type) {
        this.type = type;
    }

    public LocalDate getScheduleDate() {
        return scheduleDate;
    }

    public void setScheduleDate(LocalDate scheduleDate) {
        this.scheduleDate = scheduleDate;
    }

    public LocalTime getScheduleTime() {
        return scheduleTime;
    }

    public void setScheduleTime(LocalTime scheduleTime) {
        this.scheduleTime = scheduleTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getReminderMinutes() {
        return reminderMinutes;
    }

    public void setReminderMinutes(Integer reminderMinutes) {
        this.reminderMinutes = reminderMinutes;
    }

    public ScheduleRepeatType getRepeatType() {
        return repeatType;
    }

    public void setRepeatType(ScheduleRepeatType repeatType) {
        this.repeatType = repeatType;
    }
}

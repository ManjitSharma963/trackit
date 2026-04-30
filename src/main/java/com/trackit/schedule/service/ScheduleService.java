package com.trackit.schedule.service;

import com.trackit.auth.service.CurrentUserProvider;
import com.trackit.schedule.dto.*;
import com.trackit.schedule.model.*;
import com.trackit.schedule.repository.*;

import com.trackit.common.ResourceNotFoundException;
import com.trackit.common.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Objects;

@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final CurrentUserProvider currentUserProvider;

    public ScheduleService(ScheduleRepository scheduleRepository, CurrentUserProvider currentUserProvider) {
        this.scheduleRepository = scheduleRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public ScheduleResponse create(ScheduleRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        Schedule schedule = new Schedule();
        schedule.setUserId(userId);
        applyRequest(schedule, request);
        schedule.setStatus(ScheduleStatus.PENDING);
        return toResponse(scheduleRepository.save(schedule));
    }

    public PageResponse<ScheduleResponse> listByDate(LocalDate date, int page, int size) {
        Long userId = currentUserProvider.getCurrentUserId();
        Page<Schedule> result =
                scheduleRepository.findByUserIdAndDateOrdered(userId, date, PageRequest.of(page, size));
        return new PageResponse<>(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    public ScheduleResponse getById(Long id) {
        return toResponse(findOwnedOrThrow(id));
    }

    public ScheduleResponse update(Long id, ScheduleRequest request) {
        Schedule existing = findOwnedOrThrow(id);
        applyRequest(existing, request);
        return toResponse(scheduleRepository.save(existing));
    }

    public ScheduleResponse markDone(Long id) {
        Schedule schedule = findOwnedOrThrow(id);
        schedule.setStatus(ScheduleStatus.DONE);
        return toResponse(scheduleRepository.save(schedule));
    }

    public void delete(Long id) {
        Schedule schedule = findOwnedOrThrow(id);
        scheduleRepository.delete(schedule);
    }

    private Schedule findOwnedOrThrow(Long id) {
        Long userId = currentUserProvider.getCurrentUserId();
        Schedule schedule = scheduleRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + id));
        if (!Objects.equals(schedule.getUserId(), userId)) {
            throw new ResourceNotFoundException("Schedule not found with id: " + id);
        }
        return schedule;
    }

    private void applyRequest(Schedule schedule, ScheduleRequest request) {
        schedule.setTitle(request.getTitle());
        schedule.setType(request.getType());
        schedule.setScheduleDate(request.getScheduleDate());
        schedule.setScheduleTime(request.getScheduleTime());
        schedule.setNotes(request.getNotes());
        schedule.setReminderMinutes(request.getReminderMinutes() == null ? 0 : request.getReminderMinutes());
        schedule.setRepeatType(request.getRepeatType() == null ? ScheduleRepeatType.NONE : request.getRepeatType());
    }

    private ScheduleResponse toResponse(Schedule schedule) {
        ScheduleResponse response = new ScheduleResponse();
        response.setId(schedule.getId());
        response.setUserId(schedule.getUserId());
        response.setTitle(schedule.getTitle());
        response.setType(schedule.getType());
        response.setScheduleDate(schedule.getScheduleDate());
        response.setScheduleTime(schedule.getScheduleTime());
        response.setStatus(schedule.getStatus());
        response.setNotes(schedule.getNotes());
        response.setReminderMinutes(schedule.getReminderMinutes());
        response.setRepeatType(schedule.getRepeatType());
        response.setCreatedAt(schedule.getCreatedAt());
        response.setUpdatedAt(schedule.getUpdatedAt());
        return response;
    }
}

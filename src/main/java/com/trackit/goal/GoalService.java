package com.trackit.goals.service;

import com.trackit.goals.dto.CreateGoalRequest;
import com.trackit.goals.dto.GoalResponse;
import com.trackit.goals.dto.UpdateGoalProgressRequest;
import com.trackit.goals.model.Goal;
import com.trackit.goals.repository.GoalRepository;

import com.trackit.auth.service.CurrentUserProvider;
import com.trackit.common.PageResponse;
import com.trackit.common.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class GoalService {

    private final GoalRepository goalRepository;
    private final CurrentUserProvider currentUserProvider;

    public GoalService(GoalRepository goalRepository, CurrentUserProvider currentUserProvider) {
        this.goalRepository = goalRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public GoalResponse create(CreateGoalRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        request.setUserId(userId);
        Goal goal = new Goal();
        goal.setUserId(userId);
        goal.setTitle(request.getTitle().trim());
        goal.setDescription(trimToNull(request.getDescription()));
        goal.setTargetAmount(request.getTargetAmount().setScale(2, RoundingMode.HALF_UP));
        goal.setCurrentAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        goal.setTargetDate(request.getTargetDate());
        return toResponse(goalRepository.save(goal));
    }

    public PageResponse<GoalResponse> list(int page, int size) {
        Long userId = currentUserProvider.getCurrentUserId();
        Page<Goal> result =
                goalRepository.findByUserId(
                        userId,
                        PageRequest.of(page, size, Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"))));
        return new PageResponse<>(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional
    public GoalResponse updateProgress(Long id, UpdateGoalProgressRequest request) {
        Goal goal = findOwnedOrThrow(id);
        BigDecimal next = request.getCurrentAmount().setScale(2, RoundingMode.HALF_UP);
        goal.setCurrentAmount(next);
        return toResponse(goalRepository.save(goal));
    }

    private Goal findOwnedOrThrow(Long id) {
        Long userId = currentUserProvider.getCurrentUserId();
        Goal goal = goalRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found with id: " + id));
        if (!goal.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Goal not found with id: " + id);
        }
        return goal;
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private GoalResponse toResponse(Goal goal) {
        GoalResponse r = new GoalResponse();
        r.setId(goal.getId());
        r.setUserId(goal.getUserId());
        r.setTitle(goal.getTitle());
        r.setDescription(goal.getDescription());
        r.setTargetAmount(goal.getTargetAmount());
        r.setCurrentAmount(goal.getCurrentAmount());
        r.setProgressPercent(GoalResponse.computeProgressPercent(goal.getCurrentAmount(), goal.getTargetAmount()));
        r.setTargetDate(goal.getTargetDate());
        r.setCreatedAt(goal.getCreatedAt());
        r.setUpdatedAt(goal.getUpdatedAt());
        return r;
    }
}

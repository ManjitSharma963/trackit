package com.trackit.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class IdempotencyService {

    public enum AcquireState {
        PROCEED,
        REPLAY,
        IN_PROGRESS
    }

    public record ReplayPayload<T>(int statusCode, T body) {}

    public record AcquireResult<T>(AcquireState state, Long rowId, ReplayPayload<T> replay) {
        public static <T> AcquireResult<T> proceed(Long rowId) {
            return new AcquireResult<>(AcquireState.PROCEED, rowId, null);
        }

        public static <T> AcquireResult<T> replay(int statusCode, T body) {
            return new AcquireResult<>(AcquireState.REPLAY, null, new ReplayPayload<>(statusCode, body));
        }

        public static <T> AcquireResult<T> inProgress() {
            return new AcquireResult<>(AcquireState.IN_PROGRESS, null, null);
        }
    }

    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyKeyRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public <T> AcquireResult<T> acquire(Long userId, String key, String method, String path, Class<T> bodyType) {
        Optional<IdempotencyKey> existing =
                repository.findByUserIdAndIdempotencyKeyAndHttpMethodAndRequestPath(userId, key, method, path);
        if (existing.isPresent()) {
            return toAcquireResult(existing.get(), bodyType);
        }

        IdempotencyKey row = new IdempotencyKey();
        row.setUserId(userId);
        row.setIdempotencyKey(key);
        row.setHttpMethod(method);
        row.setRequestPath(path);
        row.setStatus(IdempotencyStatus.PROCESSING);
        try {
            IdempotencyKey saved = repository.save(row);
            return AcquireResult.proceed(saved.getId());
        } catch (DataIntegrityViolationException race) {
            IdempotencyKey winner = repository
                    .findByUserIdAndIdempotencyKeyAndHttpMethodAndRequestPath(userId, key, method, path)
                    .orElseThrow(() -> race);
            return toAcquireResult(winner, bodyType);
        }
    }

    @Transactional
    public void markCompleted(Long rowId, int responseStatus, Object responseBody) {
        IdempotencyKey row = repository.findById(rowId).orElseThrow();
        row.setStatus(IdempotencyStatus.COMPLETED);
        row.setResponseStatus(responseStatus);
        try {
            row.setResponseBody(objectMapper.writeValueAsString(responseBody));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize idempotent response body", e);
        }
        repository.save(row);
    }

    @Transactional
    public void releaseOnFailure(Long rowId) {
        repository.deleteById(rowId);
    }

    private <T> AcquireResult<T> toAcquireResult(IdempotencyKey row, Class<T> bodyType) {
        if (row.getStatus() == IdempotencyStatus.PROCESSING) {
            return AcquireResult.inProgress();
        }
        if (row.getResponseStatus() == null || row.getResponseBody() == null) {
            return AcquireResult.inProgress();
        }
        try {
            T body = objectMapper.readValue(row.getResponseBody(), bodyType);
            return AcquireResult.replay(row.getResponseStatus(), body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored idempotent response is not readable", e);
        }
    }
}

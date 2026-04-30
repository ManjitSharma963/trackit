package com.trackit.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuditLogService {

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(Long userId, String entityType, Long entityId, AuditAction action, Object oldValue, Object newValue) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction(action);

        log.setOldValueJson(toJsonQuietly(oldValue));
        log.setNewValueJson(toJsonQuietly(newValue));

        repository.save(log);
    }

    private String toJsonQuietly(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            // Audit logging must not break business operations.
            return "{\"error\":\"unserializable\"}";
        }
    }
}


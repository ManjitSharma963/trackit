package com.trackit.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Optional annotation for future AOP-based audit. Current implementation uses service-level recording
 * because we need explicit old/new snapshots.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface AuditRecord {
    String entityType();
    AuditAction action();
}


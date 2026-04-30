package com.trackit.auth.security;

/**
 * Authenticated identity extracted from JWT.
 */
public record AuthenticatedUser(Long userId, Long accountId) {}


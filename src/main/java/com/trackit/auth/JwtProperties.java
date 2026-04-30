package com.trackit.auth.security;

import com.trackit.auth.service.*;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * HMAC secret (UTF-8); must be random and at least 32 bytes for HS256.
     */
    private String secret = "change-me-in-production-use-at-least-32-chars!!";

    /**
     * Access token lifetime in milliseconds.
     */
    private long expirationMs = 86400000L;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }
}

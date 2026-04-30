package com.trackit.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class AuthResponse {

    private Long id;
    private String name;
    private String email;
    private LocalDateTime createdAt;
    /**
     * JWT access token (send as {@code Authorization: Bearer <token>}).
     */
    private String token;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @JsonProperty("accessToken")
    public String getAccessToken() {
        return token;
    }

    @JsonProperty("accessToken")
    public void setAccessToken(String accessToken) {
        this.token = accessToken;
    }
}

package com.trackit.auth.controller;

import com.trackit.auth.dto.*;
import com.trackit.auth.service.*;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signup(@Valid @RequestBody SignupRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.signup(request);
        attachAuthorizationHeader(response, authResponse);
        return authResponse;
    }

    @PostMapping("/signin")
    public AuthResponse signin(@Valid @RequestBody SigninRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.signin(request);
        attachAuthorizationHeader(response, authResponse);
        return authResponse;
    }

    /**
     * Stateless JWT: server has no session to revoke. Client must discard the token after this call.
     */
    @PostMapping("/logout")
    @PreAuthorize("hasRole('USER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        SecurityContextHolder.clearContext();
    }

    private static void attachAuthorizationHeader(HttpServletResponse response, AuthResponse authResponse) {
        if (authResponse != null && authResponse.getToken() != null && !authResponse.getToken().isBlank()) {
            response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authResponse.getToken());
        }
    }
}

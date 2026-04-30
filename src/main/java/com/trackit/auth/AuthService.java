package com.trackit.auth.service;

import com.trackit.auth.dto.*;
import com.trackit.auth.model.*;
import com.trackit.auth.repository.*;
import com.trackit.auth.security.*;

import com.trackit.account.AccountService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AccountService accountService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AccountService accountService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.accountService = accountService;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        String email = request.getEmail().trim();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }
        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        User saved = userRepository.save(user);
        Long defaultAccountId = accountService.createDefaultAccountForNewUser(saved.getId());
        return toResponse(saved, true, defaultAccountId);
    }

    @Transactional
    public AuthResponse signin(SigninRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        Long defaultAccountId = accountService.getDefaultAccountIdForUser(user.getId());
        if (defaultAccountId == null) {
            defaultAccountId = accountService.createDefaultAccountForNewUser(user.getId());
        }
        return toResponse(user, true, defaultAccountId);
    }

    private AuthResponse toResponse(User user, boolean withToken, Long accountId) {
        AuthResponse response = new AuthResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setCreatedAt(user.getCreatedAt());
        if (withToken) {
            response.setToken(jwtService.createAccessToken(user.getId(), accountId));
        }
        return response;
    }
}

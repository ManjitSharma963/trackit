package com.trackit.auth.service;

import com.trackit.auth.dto.*;
import com.trackit.auth.model.*;
import com.trackit.auth.repository.*;
import com.trackit.auth.security.*;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new UnauthorizedException("Unauthenticated");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof AuthenticatedUser user) return user.userId();
        if (principal instanceof Long userId) return userId;
        throw new UnauthorizedException("Unauthenticated");
    }

    public Long getCurrentAccountIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof AuthenticatedUser user) return user.accountId();
        return null;
    }
}

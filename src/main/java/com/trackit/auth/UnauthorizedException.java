package com.trackit.auth.security;

import com.trackit.auth.service.*;

public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}

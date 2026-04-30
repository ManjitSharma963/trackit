package com.trackit.auth.security;

import com.trackit.auth.service.*;

public final class SecurityPaths {

    private SecurityPaths() {
    }

    public static boolean isPublicPath(String requestUri) {
        if (requestUri == null) {
            return false;
        }
        String path = stripQuery(requestUri);
        if (path.equals("/api/v1/health")) {
            return true;
        }
        if (path.startsWith("/actuator")) {
            return true;
        }
        if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")) {
            return true;
        }
        return path.startsWith("/api/v1/auth/")
                || path.equals("/api/v1/auth");
    }

    private static String stripQuery(String requestUri) {
        int q = requestUri.indexOf('?');
        return q >= 0 ? requestUri.substring(0, q) : requestUri;
    }
}

package com.trackit.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

/**
 * Handles CORS by echoing the browser {@code Origin} when permitted.
 * This avoids mismatches where some stacks set {@code Access-Control-Allow-Origin}
 * to the API host (e.g. {@code http://localhost:8081}) instead of the real frontend origin.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TrackItCorsFilter extends OncePerRequestFilter {

    private final List<String> extraAllowedOrigins;

    public TrackItCorsFilter(@Value("${app.cors.allowed-origins:}") String allowedOrigins) {
        this.extraAllowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String origin = request.getHeader("Origin");

        if (isApiPath(request) && origin != null && isPermittedOrigin(origin)) {
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            response.addHeader(HttpHeaders.VARY, HttpHeaders.ORIGIN);
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            response.setHeader(
                    HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                    "GET,POST,PUT,PATCH,DELETE,OPTIONS,HEAD");
            String reqHeaders = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
            response.setHeader(
                    HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                    reqHeaders != null ? reqHeaders : "*");
            response.setHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
            response.setHeader(
                    HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                    "Authorization, X-Request-Id, Link, X-API-Version, X-API-Lifecycle");
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) && isApiPath(request)) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static boolean isApiPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String prefix = request.getContextPath() + "/api";
        return uri.startsWith(prefix);
    }

    private boolean isPermittedOrigin(String origin) {
        if (extraAllowedOrigins.contains(origin)) {
            return true;
        }
        try {
            URI uri = URI.create(origin);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (host == null) {
                return false;
            }
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                return false;
            }
            return "localhost".equalsIgnoreCase(host)
                    || "127.0.0.1".equals(host)
                    || isNgrokTunnelHost(host);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static boolean isNgrokTunnelHost(String host) {
        if (host == null) {
            return false;
        }
        String h = host.toLowerCase();
        return h.endsWith(".ngrok-free.dev")
                || h.endsWith(".ngrok-free.app")
                || h.endsWith(".ngrok.io")
                || h.endsWith(".ngrok.app");
    }

}

package com.trackit.idempotency;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class IdempotencyKeyFilter extends OncePerRequestFilter {

    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    public static final String IDEMPOTENCY_ATTR = "idempotency.key";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String m = request.getMethod();
        if (!("POST".equalsIgnoreCase(m) || "PUT".equalsIgnoreCase(m))) {
            return true;
        }
        String uri = request.getRequestURI();
        return !uri.startsWith("/api/v1/cash-entries");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String raw = request.getHeader(IDEMPOTENCY_HEADER);
        if (raw == null || raw.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        String key = raw.trim();
        if (key.length() > 128) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter()
                    .write("{\"status\":400,\"error\":\"Bad Request\",\"message\":\"Idempotency-Key too long (max 128)\"}");
            return;
        }
        request.setAttribute(IDEMPOTENCY_ATTR, key);
        filterChain.doFilter(request, response);
    }
}

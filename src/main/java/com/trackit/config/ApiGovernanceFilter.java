package com.trackit.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiGovernanceFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/v1")) {
            response.setHeader("X-API-Version", "v1");
            response.setHeader("X-API-Lifecycle", "active");
        } else if (uri.startsWith("/api/")) {
            response.setHeader("X-API-Version", "unversioned");
            response.setHeader("Deprecation", "true");
            response.setHeader("Sunset", "Tue, 31 Mar 2027 00:00:00 GMT");
            response.setHeader("Link", "</API_MIGRATION.md>; rel=\"deprecation\"; type=\"text/markdown\"");
        }
        filterChain.doFilter(request, response);
    }
}

package com.certforge.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter for /api/auth/login.
 * Blocks an IP after 10 failed login attempts within 15 minutes.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitFilter.class);
    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_MS = 15 * 60 * 1000L; // 15 minutes

    // IP → [attempt count, window start timestamp]
    private final Map<String, long[]> attempts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        if (!request.getRequestURI().equals("/api/auth/login") ||
            !request.getMethod().equals("POST")) {
            chain.doFilter(request, response);
            return;
        }

        String ip = getClientIp(request);
        long now = System.currentTimeMillis();

        attempts.compute(ip, (k, v) -> {
            if (v == null || now - v[1] > WINDOW_MS) {
                return new long[]{1, now}; // fresh window
            }
            v[0]++;
            return v;
        });

        long[] record = attempts.get(ip);
        if (record[0] > MAX_ATTEMPTS) {
            long remaining = (WINDOW_MS - (now - record[1])) / 1000 / 60;
            log.warn("🔒 Rate limit hit for IP: {} ({} attempts)", ip, (int)record[0]);
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"Too many login attempts. Try again in " + remaining + " minutes.\"}"
            );
            return;
        }

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

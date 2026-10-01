package com.procurax.identity.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Throttles authentication endpoints (`/oauth2/authorization/**`, `/login/**`) per client IP
 * using an atomic Redis INCR+EXPIRE Lua script. Fails open (allows the request but logs a
 * warning) if Redis is unreachable, so an infra blip never locks everyone out of login.
 */
@Component
@Order(1)
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimitFilter.class);

    private static final DefaultRedisScript<Long> INCR_AND_EXPIRE_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if tonumber(current) == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final int limit;
    private final int windowSeconds;

    public AuthRateLimitFilter(StringRedisTemplate redisTemplate,
                                @Value("${procurax.security.rate-limit.auth.limit}") int limit,
                                @Value("${procurax.security.rate-limit.auth.window-seconds}") int windowSeconds) {
        this.redisTemplate = redisTemplate;
        this.limit = limit;
        this.windowSeconds = windowSeconds;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.startsWith("/oauth2/authorization/") || path.startsWith("/login/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = "procurax:ratelimit:auth:" + clientIp(request);
        try {
            Long count = redisTemplate.execute(INCR_AND_EXPIRE_SCRIPT, List.of(key), String.valueOf(windowSeconds));
            if (count != null && count > limit) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"status\":429,\"code\":\"RATE_LIMITED\",\"message\":\"Too many authentication attempts\"}");
                return;
            }
        } catch (Exception ex) {
            log.warn("Rate limiter unavailable, failing open: {}", ex.getMessage());
        }
        chain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

package com.example.pizza.logic.interceptor;

import com.example.pizza.config.performance.RateLimitConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitConfig rateLimitConfig;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        // Skip if rate limiting is disabled
        if (!rateLimitConfig.isEnabled()) {
            return true;
        }

        // Skip for actuator endpoints
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/actuator")) {
            return true;
        }

        // Get rate limit key (userId or IP)
        String key = getRateLimitKey(request);

        // Resolve bucket (endpoint-specific or default)
        Bucket bucket = rateLimitConfig.resolveBucket(key, requestURI);

        // Try to consume 1 token
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Request allowed - add rate limit headers
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            log.debug("Rate limit check passed for key: {} on endpoint: {} (remaining: {})",
                    key, requestURI, probe.getRemainingTokens());
            return true;
        } else {
            // Rate limit exceeded
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000; // Convert to seconds

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                    "{\"error\": \"Too Many Requests\", " +
                            "\"message\": \"Rate limit exceeded. Please try again in %d seconds.\", " +
                            "\"retryAfter\": %d}",
                    waitForRefill, waitForRefill
            ));

            log.warn("Rate limit exceeded for key: {} on endpoint: {} (retry after: {}s)",
                    key, requestURI, waitForRefill);

            return false;
        }
    }

    private String getRateLimitKey(HttpServletRequest request) {
        // Try to get authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getPrincipal().equals("anonymousUser")) {
            // Use userId for authenticated users
            String userId = authentication.getName();
            log.debug("Rate limit key: userId={}", userId);
            return "user:" + userId;
        }

        // Use IP address for anonymous users
        String ipAddress = getClientIP(request);
        log.debug("Rate limit key: IP={}", ipAddress);
        return "ip:" + ipAddress;
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }

        // Fallback to remote address
        return request.getRemoteAddr();
    }
}
package com.example.pizza.config.performance;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@ConfigurationProperties(prefix = "app.rate-limit")
@Getter
public class RateLimitConfig {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    private boolean enabled = true;
    private int capacity = 100;
    private int refillTokens = 100;
    private long refillDurationMinutes = 1;

    private Map<String, EndpointLimit> endpoints = Map.of(
            "/api/auth/login", new EndpointLimit(10, 1),
            "/api/auth/register", new EndpointLimit(5, 1),
            "/api/orders", new EndpointLimit(50, 1),
            "/api/product", new EndpointLimit(200, 1),
            "/api/category", new EndpointLimit(200, 1)
    );

    public Bucket resolveBucket(String key) {
        return cache.computeIfAbsent(key, k -> createNewBucket(capacity, refillTokens, refillDurationMinutes));
    }

    public Bucket resolveBucket(String key, String endpoint) {
        EndpointLimit limit = endpoints.get(endpoint);
        if (limit != null) {
            String bucketKey = key + ":" + endpoint;
            return cache.computeIfAbsent(bucketKey,
                    k -> createNewBucket(limit.getCapacity(), limit.getCapacity(), limit.getRefillMinutes()));
        }
        return resolveBucket(key);
    }


    private Bucket createNewBucket(int capacity, int refillTokens, long refillMinutes) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(refillTokens, Duration.ofMinutes(refillMinutes))
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public EndpointLimit getEndpointLimit(String endpoint) {
        return endpoints.get(endpoint);
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Getter
    public static class EndpointLimit {
        private final int capacity;
        private final long refillMinutes;

        public EndpointLimit(int capacity, long refillMinutes) {
            this.capacity = capacity;
            this.refillMinutes = refillMinutes;
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public void setRefillTokens(int refillTokens) {
        this.refillTokens = refillTokens;
    }

    public void setRefillDurationMinutes(long refillDurationMinutes) {
        this.refillDurationMinutes = refillDurationMinutes;
    }

    public void setEndpoints(Map<String, EndpointLimit> endpoints) {
        this.endpoints = endpoints;
    }
}
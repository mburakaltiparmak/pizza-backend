package com.example.pizza.config.actuator;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class ActuatorConfig {

    @Bean
    public HttpExchangeRepository httpExchangeRepository() {
        return new InMemoryHttpExchangeRepository();
    }

    @Component
    public static class DatabaseHealthIndicator implements HealthIndicator {

        @Autowired(required = false)
        private JdbcTemplate jdbcTemplate;

        @Override
        public Health health() {
            if (jdbcTemplate == null) {
                return Health.unknown()
                        .withDetail("database", "No JdbcTemplate configured")
                        .build();
            }

            try {
                long startTime = System.currentTimeMillis();
                Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
                long responseTime = System.currentTimeMillis() - startTime;

                if (result != null && result == 1) {
                    return Health.up()
                            .withDetail("database", "PostgreSQL")
                            .withDetail("responseTime", responseTime + "ms")
                            .withDetail("status", "Connection successful")
                            .build();
                } else {
                    return Health.down()
                            .withDetail("database", "PostgreSQL")
                            .withDetail("error", "Unexpected query result")
                            .build();
                }
            } catch (Exception e) {
                return Health.down()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        }
    }

    @Component
    public static class CustomInfoContributor implements InfoContributor {

        @Value("${spring.application.name:Pizza Delivery API}")
        private String appName;

        @Value("${app.version:1.0.0}")
        private String appVersion;

        @Value("${info.app.developer:Your Name}")
        private String developer;

        @Value("${info.app.contact:info@your-domain.com}")
        private String contact;

        @Value("${info.app.portfolio:https://your-portfolio.com}")
        private String portfolio;

        @Override
        public void contribute(Info.Builder builder) {
            Map<String, Object> appDetails = new HashMap<>();
            appDetails.put("name", appName);
            appDetails.put("description", "RESTful API for Pizza Restaurant Website System");
            appDetails.put("version", appVersion);
            appDetails.put("developer", developer);
            appDetails.put("contact", contact);
            appDetails.put("portfolio", portfolio);
            appDetails.put("environment", System.getenv("FLY_APP_NAME") != null ? "fly.io" : "local");

            Map<String, Object> deployment = new HashMap<>();
            deployment.put("platform", "Fly.io");
            deployment.put("region", System.getenv("FLY_REGION"));
            deployment.put("instance", System.getenv("FLY_ALLOC_ID"));

            Map<String, Object> features = new HashMap<>();
            features.put("authentication", "JWT Token Based and Google oAuth on Supabase");
            features.put("database", "PostgreSQL on Supabase");
            features.put("fileStorage", "Cloudinary");
            features.put("deployment", "Docker on Fly.io");
            features.put("monitoring", "Spring Boot Actuator");

            builder.withDetail("application", appDetails)
                    .withDetail("deployment", deployment)
                    .withDetail("features", features)
                    .withDetail("buildTime", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        }
    }

    @Component
    @Endpoint(id = "app-stats")
    @ConditionalOnAvailableEndpoint(endpoint = ApplicationStatisticsEndpoint.class)
    public static class ApplicationStatisticsEndpoint {

        private final AtomicLong requestCount = new AtomicLong(0);
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong errorCount = new AtomicLong(0);
        private final Map<String, Long> endpointStats = new ConcurrentHashMap<>();
        private final LocalDateTime startTime = LocalDateTime.now();

        @ReadOperation
        public Map<String, Object> getStatistics() {
            Map<String, Object> stats = new HashMap<>();

            // Genel istatistikler
            stats.put("totalRequests", requestCount.get());
            stats.put("successfulRequests", successCount.get());
            stats.put("failedRequests", errorCount.get());
            stats.put("successRate", calculateSuccessRate());
            stats.put("uptime", calculateUptime());
            stats.put("startTime", startTime.format(DateTimeFormatter.ISO_DATE_TIME));

            // En çok kullanılan endpoint'ler
            List<Map.Entry<String, Long>> topEndpoints = endpointStats.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(10)
                    .toList();

            stats.put("topEndpoints", topEndpoints);
            stats.put("totalEndpoints", endpointStats.size());

            // Sistem kaynakları
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> systemInfo = new HashMap<>();
            systemInfo.put("totalMemory", formatBytes(runtime.totalMemory()));
            systemInfo.put("freeMemory", formatBytes(runtime.freeMemory()));
            systemInfo.put("usedMemory", formatBytes(runtime.totalMemory() - runtime.freeMemory()));
            systemInfo.put("maxMemory", formatBytes(runtime.maxMemory()));
            systemInfo.put("availableProcessors", runtime.availableProcessors());

            // Fly.io specific info
            if (System.getenv("FLY_APP_NAME") != null) {
                Map<String, String> flyInfo = new HashMap<>();
                flyInfo.put("app", System.getenv("FLY_APP_NAME"));
                flyInfo.put("region", System.getenv("FLY_REGION"));
                flyInfo.put("instance", System.getenv("FLY_ALLOC_ID"));
                systemInfo.put("fly", flyInfo);
            }

            stats.put("system", systemInfo);

            return stats;
        }

        @WriteOperation
        public Map<String, Object> resetStatistics() {
            requestCount.set(0);
            successCount.set(0);
            errorCount.set(0);
            endpointStats.clear();

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Statistics reset successfully");
            response.put("resetTime", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            return response;
        }

        @ReadOperation
        public Map<String, Object> getEndpointStatistics(@Selector String endpoint) {
            Map<String, Object> stats = new HashMap<>();
            stats.put("endpoint", endpoint);
            stats.put("requestCount", endpointStats.getOrDefault(endpoint, 0L));
            return stats;
        }

        private String calculateSuccessRate() {
            long total = requestCount.get();
            if (total == 0)
                return "0%";
            double rate = (successCount.get() * 100.0) / total;
            return String.format("%.2f%%", rate);
        }

        private String calculateUptime() {
            LocalDateTime now = LocalDateTime.now();
            long hours = java.time.Duration.between(startTime, now).toHours();
            long days = hours / 24;
            hours = hours % 24;
            return String.format("%d days, %d hours", days, hours);
        }

        private String formatBytes(long bytes) {
            if (bytes < 1024)
                return bytes + " B";
            int exp = (int) (Math.log(bytes) / Math.log(1024));
            String pre = "KMGTPE".charAt(exp - 1) + "";
            return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
        }

        public void incrementRequestCount() {
            requestCount.incrementAndGet();
        }

        public void incrementSuccessCount() {
            successCount.incrementAndGet();
        }

        public void incrementErrorCount() {
            errorCount.incrementAndGet();
        }

        public void updateEndpointStats(String endpoint) {
            endpointStats.merge(endpoint, 1L, Long::sum);
        }
    }

    @Component
    public static class ApiHealthIndicator implements HealthIndicator {

        @Autowired(required = false)
        private ApplicationStatisticsEndpoint statsEndpoint;

        @Override
        public Health health() {
            if (statsEndpoint == null) {
                return Health.up()
                        .withDetail("api", "No statistics available")
                        .build();
            }

            Map<String, Object> stats = statsEndpoint.getStatistics();
            Long totalRequests = (Long) stats.get("totalRequests");
            String successRate = (String) stats.get("successRate");
            String uptime = (String) stats.get("uptime");

            Health.Builder builder;

            // Success rate'e göre health durumu
            // İlk başlangıçta (totalRequests = 0) UP olmalı
            if (totalRequests != null && totalRequests > 0 && successRate != null) {
                double rate = Double.parseDouble(successRate.replace("%", ""));
                if (rate < 70.0) {
                    builder = Health.down()
                            .withDetail("reason", "Low success rate");
                } else if (rate < 90.0) {
                    // Degraded state yoksa, warning olarak up state kullan
                    builder = Health.up()
                            .withDetail("warning", "Success rate below 90%");
                } else {
                    builder = Health.up();
                }
            } else {
                // Henüz request yoksa veya successRate null ise UP
                builder = Health.up();
            }

            return builder
                    .withDetail("totalRequests", totalRequests != null ? totalRequests : 0)
                    .withDetail("successRate", successRate != null ? successRate : "N/A")
                    .withDetail("uptime", uptime != null ? uptime : "N/A")
                    .withDetail("status", "API is operational")
                    .build();
        }
    }
}
package com.example.pizza.service.logic;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class AuditLogService {

    public void logUserAction(String username, String action, String details) {
        log.info("User Action [{}]: {} - {} at {}", username, action, details, LocalDateTime.now());
    }

    public void logProductAction(String username, String action, Long productId) {
        log.info("Product Action [{}]: {} product ID {} at {}", username, action, productId, LocalDateTime.now());
    }

    public void logCategoryAction(String username, String action, Long categoryId) {
        log.info("Category Action [{}]: {} category ID {} at {}", username, action, categoryId, LocalDateTime.now());
    }

    public void logOrderAction(String username, String action, Long orderId) {
        log.info("Order Action [{}]: {} order ID {} at {}", username, action, orderId, LocalDateTime.now());
    }

    public void logAuthAction(String username, String action, String ipAddress) {
        log.info("Auth Action [{}]: {} from IP {} at {}", username, action, ipAddress, LocalDateTime.now());
    }

    public void logError(String source, String error) {
        log.error("Error in {}: {} at {}", source, error, LocalDateTime.now());
    }
}
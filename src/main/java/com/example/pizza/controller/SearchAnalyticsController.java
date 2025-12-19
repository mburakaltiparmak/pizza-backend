package com.example.pizza.controller;

import com.example.pizza.dto.admin.SearchAnalyticsResponse;
import com.example.pizza.service.admin.SearchAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/analytics")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SearchAnalyticsController {

    private final SearchAnalyticsService searchAnalyticsService;

    /**
     * Get search analytics
     *
     * GET /api/admin/analytics/search?days=30
     * TODO Implement Apache Airflow
     * @param days Number of days to analyze (default: 7)
     * @return Search analytics data
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'PERSONAL')")
    public ResponseEntity<SearchAnalyticsResponse> getSearchAnalytics(
            @RequestParam(defaultValue = "7") int days) {

        log.info("Admin analytics request: days={}", days);

        SearchAnalyticsResponse analytics = searchAnalyticsService.getSearchAnalytics(days);

        return ResponseEntity.ok(analytics);
    }
}

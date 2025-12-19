package com.example.pizza.service.admin;

import com.example.pizza.dto.admin.SearchAnalyticsResponse;
import com.example.pizza.entity.logic.SearchLog;
import com.example.pizza.entity.user.User;
import com.example.pizza.repository.CategoryRepository;
import com.example.pizza.repository.SearchLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchAnalyticsService {

    private final SearchLogRepository searchLogRepository;
    private final CategoryRepository categoryRepository;

    // ============================================================================
    // SEARCH LOGGING (Async - Non-blocking)
    // ============================================================================

    @Async
    @Transactional
    public void logSearch(
            String query,
            Integer resultCount,
            String searchType,
            User user,
            Long categoryId,
            Double minPrice,
            Double maxPrice,
            HttpServletRequest request,
            Long responseTimeMs) {

        try {
            SearchLog searchLog = SearchLog.builder()
                    .query(query != null ? query.toLowerCase().trim() : "")
                    .resultCount(resultCount)
                    .searchType(searchType)
                    .user(user)
                    .categoryId(categoryId)
                    .minPrice(minPrice)
                    .maxPrice(maxPrice)
                    .ipAddress(getClientIpAddress(request))
                    .userAgent(request.getHeader("User-Agent"))
                    .responseTimeMs(responseTimeMs)
                    .build();

            searchLogRepository.save(searchLog);

            log.debug("Search logged: query={}, results={}, type={}", query, resultCount, searchType);
        } catch (Exception e) {
            // Non-blocking: Do not throw exception
            log.error("Failed to log search: query={}", query, e);
        }
    }

    // ============================================================================
    // ANALYTICS GENERATION
    // ============================================================================

    @Transactional(readOnly = true)
    public SearchAnalyticsResponse getSearchAnalytics(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        // 1. Total Stats
        Long totalSearches = searchLogRepository.countSearchesSince(startDate);
        Double avgResponseTime = searchLogRepository.findAverageResponseTime(startDate);

        SearchAnalyticsResponse.TotalStats totalStats = new SearchAnalyticsResponse.TotalStats(
                totalSearches,
                0L, // TODO: Calculate unique queries
                0.0 // TODO: Calculate average result count
        );

        // 2. Top Search Queries
        List<SearchAnalyticsResponse.QueryStat> topQueries = searchLogRepository
                .findTopSearchQueries(startDate)
                .stream()
                .map(row -> new SearchAnalyticsResponse.QueryStat(
                        (String) row[0],
                        ((Number) row[1]).longValue()))
                .collect(Collectors.toList());

        // 3. Zero-Result Queries
        List<SearchAnalyticsResponse.QueryStat> zeroResultQueries = searchLogRepository
                .findZeroResultQueries(startDate)
                .stream()
                .map(row -> new SearchAnalyticsResponse.QueryStat(
                        (String) row[0],
                        ((Number) row[1]).longValue()))
                .collect(Collectors.toList());

        // 4. Searches by Category
        List<SearchAnalyticsResponse.CategoryStat> categoryStats = searchLogRepository
                .findSearchCountByCategory(startDate)
                .stream()
                .map(row -> {
                    Long categoryId = ((Number) row[0]).longValue();
                    String categoryName = categoryRepository.findById(categoryId)
                            .map(cat -> cat.getName())
                            .orElse("Unknown");
                    Long count = ((Number) row[1]).longValue();

                    return new SearchAnalyticsResponse.CategoryStat(categoryId, categoryName, count);
                })
                .collect(Collectors.toList());

        // 5. Performance Stats
        SearchAnalyticsResponse.PerformanceStats performanceStats = new SearchAnalyticsResponse.PerformanceStats(
                avgResponseTime != null ? avgResponseTime : 0.0,
                0L, // TODO: Calculate slowest query
                0L // TODO: Calculate fastest query
        );

        return new SearchAnalyticsResponse(
                totalStats,
                topQueries,
                zeroResultQueries,
                categoryStats,
                performanceStats);
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }
}

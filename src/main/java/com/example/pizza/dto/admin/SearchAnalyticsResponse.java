package com.example.pizza.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchAnalyticsResponse implements Serializable {

    private TotalStats totalStats;
    private List<QueryStat> topQueries;
    private List<QueryStat> zeroResultQueries;
    private List<CategoryStat> categoryStats;
    private PerformanceStats performanceStats;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TotalStats implements Serializable {
        private Long totalSearches;
        private Long uniqueQueries;
        private Double averageResults;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueryStat implements Serializable {
        private String query;
        private Long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStat implements Serializable {
        private Long categoryId;
        private String categoryName;
        private Long searchCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceStats implements Serializable {
        private Double averageResponseTime;
        private Long slowestQuery;
        private Long fastestQuery;
    }
}

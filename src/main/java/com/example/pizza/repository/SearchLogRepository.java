package com.example.pizza.repository;

import com.example.pizza.entity.logic.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    @Query("SELECT s.query, COUNT(s) as count FROM SearchLog s " +
            "WHERE s.createdAt >= :startDate " +
            "GROUP BY s.query " +
            "ORDER BY count DESC")
    List<Object[]> findTopSearchQueries(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT s.query, COUNT(s) as count FROM SearchLog s " +
            "WHERE s.createdAt >= :startDate AND s.resultCount = 0 " +
            "GROUP BY s.query " +
            "ORDER BY count DESC")
    List<Object[]> findZeroResultQueries(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT s.categoryId, COUNT(s) as count FROM SearchLog s " +
            "WHERE s.createdAt >= :startDate AND s.categoryId IS NOT NULL " +
            "GROUP BY s.categoryId " +
            "ORDER BY count DESC")
    List<Object[]> findSearchCountByCategory(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT AVG(s.responseTimeMs) FROM SearchLog s " +
            "WHERE s.createdAt >= :startDate AND s.responseTimeMs IS NOT NULL")
    Double findAverageResponseTime(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(s) FROM SearchLog s WHERE s.createdAt >= :startDate")
    Long countSearchesSince(@Param("startDate") LocalDateTime startDate);
}

package com.example.pizza.entity.logic;

import com.example.pizza.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_logs", indexes = {
        @Index(name = "idx_search_query", columnList = "query"),
        @Index(name = "idx_search_created", columnList = "createdAt"),
        @Index(name = "idx_search_category", columnList = "categoryId")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String query;

    @Column(nullable = false)
    private Integer resultCount;

    @Column(length = 50)
    private String searchType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "min_price")
    private Double minPrice;

    @Column(name = "max_price")
    private Double maxPrice;

    @Column(length = 45) // IPv6 support
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

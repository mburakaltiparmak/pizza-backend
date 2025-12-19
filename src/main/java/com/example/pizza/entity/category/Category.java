package com.example.pizza.entity.category;

import com.example.pizza.entity.product.Product;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(schema = "pizza", name = "category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Kategori adı boş olamaz")
    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @Column(name = "img")
    private String img;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = false)
    @JsonManagedReference
    private List<Product> products = new ArrayList<>();

    // ========================================================================
    // TIMESTAMP FIELD (FIXED - Added for pagination sorting)
    // ========================================================================

    /**
     * Kategori oluşturulma zamanı
     * Database: created_at TIMESTAMP
     * JPA: createdAt LocalDateTime
     *
     * Allows sorting: ?sort=createdAt,desc
     */
    @Column(name = "created_at")
    private LocalDate createdAt;

    // ========================================================================
    // LIFECYCLE CALLBACKS
    // ========================================================================

    /**
     * Entity persist edilmeden önce created_at set et
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDate.now();
        }
    }
}

/**
 * ============================================================================
 * CHANGELOG
 * ============================================================================
 *
 * - Added createdAt field with @Column(name = "created_at")
 * - Added @PrePersist callback for auto-setting timestamp
 *
 * Note: Category table only has created_at, not updated_at
 *
 * Benefits:
 * - Pagination sort by createdAt now works
 * - Automatic timestamp management via JPA callback
 * - Consistent with database schema
 *
 * Usage Examples:
 * - GET /api/category/paged?sort=createdAt,desc  ← Now works!
 * - GET /api/category/paged?sort=id,asc          ← Still works!
 *
 * ============================================================================
 */
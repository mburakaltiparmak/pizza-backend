package com.example.pizza.entity.product;

import com.example.pizza.entity.category.Category;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * FIXED: Added created_at and updated_at timestamp fields
 * Issue: Pagination sort by createdAt was failing because field didn't exist in entity
 */
@Entity
@Data
@Table(schema = "pizza", name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "Ürün adı boş olamaz")
    @Size(max = 255, message = "Ürün adı en fazla 255 karakter olabilir")
    @Column(name = "name", nullable = false)
    private String name;

    @Min(value = 0, message = "Puan 0'dan küçük olamaz")
    @Max(value = 5, message = "Puan 5'ten büyük olamaz")
    @Column(name = "rating")
    private double rating;

    @Min(value = 0, message = "Stok miktarı negatif olamaz")
    @Column(name = "stock")
    private int stock;

    @Positive(message = "Fiyat sıfırdan büyük olmalıdır")
    @Column(name = "price")
    private double price;

    @NotBlank(message = "Ürün resmi boş olamaz")
    @Column(name = "img")
    private String img;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    @NotNull(message = "Kategori boş olamaz")
    @JsonBackReference
    private Category category;

    // ========================================================================
    // TIMESTAMP FIELDS (FIXED - Added for pagination sorting)
    // ========================================================================

    /**
     * Ürün oluşturulma zamanı
     * Database: created_at TIMESTAMP
     * JPA: createdAt LocalDateTime
     *
     * Allows sorting: ?sort=createdAt,desc
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Ürün güncellenme zamanı  
     * Database: updated_at TIMESTAMP
     * JPA: updatedAt LocalDateTime
     *
     * Allows sorting: ?sort=updatedAt,desc
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ========================================================================
    // LIFECYCLE CALLBACKS (Optional - Auto-set timestamps)
    // ========================================================================

    /**
     * Entity persist edilmeden önce created_at ve updated_at set et
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Entity update edilmeden önce updated_at güncelle
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

/**
 * ============================================================================
 * CHANGELOG
 * ============================================================================
 *
 * Version 4.5.2 - 23 Kasım 2025
 * - Added createdAt field with @Column(name = "created_at")
 * - Added updatedAt field with @Column(name = "updated_at")
 * - Added @PrePersist callback for auto-setting timestamps
 * - Added @PreUpdate callback for auto-updating updatedAt
 *
 * Benefits:
 * - Pagination sort by createdAt now works
 * - Sort by updatedAt also available
 * - Automatic timestamp management via JPA callbacks
 * - Consistent with database schema (init-schema.sql)
 *
 * Usage Examples:
 * - GET /api/product/paged?sort=createdAt,desc  ← Now works!
 * - GET /api/product/paged?sort=updatedAt,asc   ← Also works!
 * - GET /api/product/paged?sort=id,desc         ← Still works!
 *
 * ============================================================================
 */
package com.example.pizza.repository;

import com.example.pizza.dto.product.ProductSummaryDTO;
import com.example.pizza.entity.category.Category;
import com.example.pizza.entity.product.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id IN :ids")
    List<Product> findByIdsWithLock(@Param("ids") List<Long> ids);

    List<Product> findByCategory(Category category);

    List<Product> findByNameContainingIgnoreCase(String keyword);

    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    List<Product> findByRatingGreaterThanEqual(Double rating);

    List<Product> findByStockGreaterThan(Integer stock);

    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId " +
            "AND p.price BETWEEN :minPrice AND :maxPrice " +
            "AND p.rating >= :minRating")
    List<Product> findByCategoryWithFilters(
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minRating") Double minRating);

    @Query("SELECT COALESCE(SUM(p.stock), 0) FROM Product p")
    long calculateTotalStock();

    /**
     * Kategoriye göre ürünleri getir (WITHOUT PAGINATION)
     *
     * @deprecated Use {@link #findByCategoryId(Long, Pageable)} for pagination
     */
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId")
    List<Product> findByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * Dashboard için ürün summary'leri (WITHOUT PAGINATION)
     */
    @Query(value = """
            SELECT new com.example.pizza.dto.product.ProductSummaryDTO(
                p.id,
                p.name,
                p.rating,
                p.stock,
                p.price,
                p.img,
                p.description,
                p.category.id
            )
            FROM Product p
            WHERE p.category.id = :categoryId
            """)
    List<ProductSummaryDTO> findProductSummariesByCategory(@Param("categoryId") Long categoryId);

    /**
     * En son eklenen ürünleri getir (WITHOUT PAGINATION)
     */
    @Query(value = """
            SELECT new com.example.pizza.dto.product.ProductSummaryDTO(
                p.id,
                p.name,
                p.rating,
                p.stock,
                p.price,
                p.img,
                p.description,
                p.category.id
            )
            FROM Product p
            ORDER BY p.id DESC
            LIMIT :limit
            """)
    List<ProductSummaryDTO> findRecentProducts(@Param("limit") int limit);

    /**
     * Kategoriye göre ürün sayısı
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId")
    long countByCategory(@Param("categoryId") Long categoryId);

    /**
     * Kategoriye göre toplam stok
     */
    @Query("SELECT COALESCE(SUM(p.stock), 0) FROM Product p WHERE p.category.id = :categoryId")
    long calculateTotalStockByCategory(@Param("categoryId") Long categoryId);

    /**
     * Stokta olmayan ürün sayısı
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.stock = 0")
    long countOutOfStockProducts();

    /**
     * Düşük stoklu ürün sayısı
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.stock > 0 AND p.stock < :threshold")
    long countLowStockProducts(@Param("threshold") int threshold);

    // ============================================================================
    // PAGINATION METHODS
    // ============================================================================

    /**
     * Kategoriye göre ürünleri sayfalı olarak getir
     *
     * @param categoryId Kategori ID
     * @param pageable   Pagination parametreleri
     * @return Paginated products
     */
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId")
    Page<Product> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

    /**
     * İsme göre ürün arama (sayfalı)
     *
     * @param name     Aranacak isim (partial match)
     * @param pageable Pagination parametreleri
     * @return Paginated search results
     */
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Product> searchByName(@Param("name") String name, Pageable pageable);

    /**
     * Fiyat aralığına göre ürün arama (sayfalı)
     *
     * @param minPrice Minimum fiyat
     * @param maxPrice Maximum fiyat
     * @param pageable Pagination parametreleri
     * @return Paginated products in price range
     */
    @Query("SELECT p FROM Product p WHERE p.price BETWEEN :minPrice AND :maxPrice")
    Page<Product> findByPriceRange(
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            Pageable pageable);

    /**
     * Stok durumuna göre ürünleri getir (sayfalı)
     *
     * HQL SYNTAX FIXED: Boolean comparison using CASE WHEN
     *
     * @param inStock  true = stokta var, false = stokta yok
     * @param pageable Pagination parametreleri
     * @return Paginated products
     */
    @Query("SELECT p FROM Product p WHERE " +
            "CASE WHEN :inStock = true THEN p.stock > 0 " +
            "ELSE p.stock = 0 END = true")
    Page<Product> findByStockAvailability(@Param("inStock") boolean inStock, Pageable pageable);
}
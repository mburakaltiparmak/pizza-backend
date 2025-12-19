package com.example.pizza.repository;

import com.example.pizza.entity.category.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

        Optional<Category> findByName(String name);

        // ============================================================================
        // LEGACY METHODS (Backward Compatibility)
        // ============================================================================

        /**
         * N+1 çözümü: Products ile birlikte getir (WITHOUT PAGINATION)
         *
         * @deprecated Use {@link #findAllWithProducts(Pageable)} instead for pagination
         * @return Tüm kategoriler products ile
         */
        @Query("SELECT DISTINCT c FROM Category c " +
                        "LEFT JOIN FETCH c.products " +
                        "ORDER BY c.id")
        List<Category> findAllWithProducts();

        /**
         * Tekil kategori için products ile birlikte getir
         *
         * @param id Kategori ID
         * @return Category with products
         */
        @Query("SELECT c FROM Category c " +
                        "LEFT JOIN FETCH c.products " +
                        "WHERE c.id = :id")
        Optional<Category> findByIdWithProducts(@Param("id") Long id);

        // ============================================================================
        // PAGINATION METHODS
        // ============================================================================

        /**
         * Kategorileri sayfalı olarak getir (products dahil)
         *
         * NOT: JOIN FETCH ile pagination kullanımı dikkatli olmalı!
         * Bu query, memory'de join yapıp sonra paginate eder.
         * Daha iyi performans için products'ı ayrı query ile getirmek önerilir.
         *
         * Kullanım:
         * Pageable pageable = PageRequest.of(0, 10, Sort.by("name").ascending());
         * Page<Category> categories = repository.findAllWithProducts(pageable);
         *
         * @param pageable Pagination parametreleri
         * @return Paginated categories with products
         */
        @Query(value = "SELECT DISTINCT c FROM Category c " +
                        "LEFT JOIN FETCH c.products " +
                        "ORDER BY c.id", countQuery = "SELECT COUNT(DISTINCT c) FROM Category c")
        Page<Category> findAllWithProducts(Pageable pageable);

        /**
         * Kategorileri sayfalı olarak getir (products OLMADAN - Daha performanslı)
         *
         * Bu method JpaRepository'den otomatik gelir ama açıklama için yazdık.
         * Products'ı lazy loading ile veya ayrı query ile getirmek daha iyi.
         *
         * Kullanım:
         * Pageable pageable = PageRequest.of(0, 10, Sort.by("name"));
         * Page<Category> categories = repository.findAll(pageable);
         *
         * @param pageable Pagination parametreleri
         * @return Paginated categories without products
         */
        // findAll(Pageable) otomatik gelir JpaRepository'den, override etmeye gerek yok

        /**
         * İsme göre kategori arama (sayfalı)
         *
         * Case-insensitive partial match yapılır.
         *
         * Kullanım:
         * Pageable pageable = PageRequest.of(0, 20);
         * Page<Category> categories = repository.searchByName("pizza", pageable);
         *
         * @param name     Aranacak isim (partial match)
         * @param pageable Pagination parametreleri
         * @return Paginated search results
         */
        @Query("SELECT c FROM Category c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))")
        Page<Category> searchByName(@Param("name") String name, Pageable pageable);
}
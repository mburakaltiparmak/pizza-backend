package com.example.pizza.service.category;

import com.example.pizza.dto.category.CategoryResponse;
import com.example.pizza.entity.category.Category;
import com.example.pizza.entity.product.Product;
import com.example.pizza.exceptions.common.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface CategoryService {

    // ============================================================================
    // LEGACY METHODS (Backward Compatibility)
    // ============================================================================
    List<CategoryResponse> getAllCategories();
    Category findById(Long id);
    Category findByName(String name);
    Category save(Category category, MultipartFile file) throws IOException;
    Category update(Long id, Category category, MultipartFile file) throws IOException;
    void delete(Long id);
    List<Product> getAllProductsByCategory(Long categoryId);
    List<Category> getAllCategoriesWithProducts();

    // ============================================================================
    // PAGINATION METHODS (DTO-BASED)
    // ============================================================================

    /**
     * Tüm kategorileri sayfalı olarak getir - DTO döndürür ve cache edilir
     *
     * Return type changed to Page<CategoryResponse> for Redis caching
     *
     * Kullanım:
     * Pageable pageable = PageRequest.of(0, 10, Sort.by("name").ascending());
     * Page<CategoryResponse> categories = service.getAllCategories(pageable);
     *
     * @param pageable Pagination parametreleri (page, size, sort)
     * @return Page<CategoryResponse> - Paginated DTO (cacheable)
     */
    Page<CategoryResponse> getAllCategories(Pageable pageable);

    /**
     * Kategorileri products ile birlikte sayfalı getir
     *
     * NOT: Products eager loading yapıldığı için performans dikkatli kullanılmalı.
     * Alternatif: Products'ı ayrı endpoint'ten lazy load et.
     *
     * @param pageable Pagination parametreleri
     * @return Paginated categories with products
     */
    Page<Category> getAllCategoriesWithProducts(Pageable pageable);

    /**
     * İsme göre kategori arama (sayfalı)
     *
     * Case-insensitive partial match yapılır.
     *
     * @param name Aranacak kategori adı (partial)
     * @param pageable Pagination parametreleri
     * @return Paginated search results
     */
    Page<Category> searchByName(String name, Pageable pageable);
}
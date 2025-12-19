package com.example.pizza.controller;

import com.example.pizza.constants.logic.DatabaseConstants;
import com.example.pizza.dto.category.CategoryResponse;
import com.example.pizza.dto.paginate.PagedResponse;
import com.example.pizza.entity.category.Category;
import com.example.pizza.service.category.CategorySearchService;
import com.example.pizza.service.category.CategoryService;
import com.example.pizza.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/category")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CategoryRestController {

    private final CategoryService categoryService;
    private final ProductService productService;
    private final CategorySearchService categorySearchService;

    // ============================================================================
    // LEGACY ENDPOINTS (Backward Compatibility)
    // ============================================================================

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> findAll() {
        try {
            List<CategoryResponse> categories = categoryService.getAllCategories();

            List<CategoryResponse> filtered = categories.stream()
                    .filter(c -> c != null &&
                            !DatabaseConstants.CUSTOM_CATEGORY_NAME.getStringValue().equals(c.name()))
                    .toList();

            return ResponseEntity.ok(filtered);

        } catch (Exception e) {
            System.err.println("Error fetching categories: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> findById(@PathVariable Long id) {
        try {
            Category category = categoryService.findById(id);
            CategoryResponse response = new CategoryResponse(
                    category.getId(),
                    category.getName(),
                    category.getImg());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> save(
            @RequestParam("name") String name,
            @RequestParam(value = "image", required = false) MultipartFile file) {
        try {
            Category category = new Category();
            category.setName(name);
            Category savedCategory = categoryService.save(category, file);
            CategoryResponse response = new CategoryResponse(
                    savedCategory.getId(),
                    savedCategory.getName(),
                    savedCategory.getImg());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam(value = "img", required = false) MultipartFile file) {
        try {
            Category category = new Category();
            category.setName(name);
            Category updatedCategory = categoryService.update(id, category, file);
            CategoryResponse response = new CategoryResponse(
                    updatedCategory.getId(),
                    updatedCategory.getName(),
                    updatedCategory.getImg());
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            categoryService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/products")
    public ResponseEntity<?> getProductsByCategory(@PathVariable Long id) {
        try {
            Category category = categoryService.findById(id);
            return ResponseEntity.ok(productService.getProductsByCategory(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============================================================================
    // PAGINATION ENDPOINT (SIMPLIFIED WITH CACHE)
    // ============================================================================

    /**
     * Kategorileri sayfalı olarak getir
     *
     * Service direkt Page<CategoryResponse> döndürür (CACHED)
     * Controller'da Entity → DTO dönüşümüne gerek yok
     *
     * Endpoint: GET /api/category/paged?page=0&size=20&sort=name,asc
     *
     * @param pageable Pagination parametreleri (page, size, sort)
     * @return PagedResponse<CategoryResponse> - Paginated categories (cached)
     */
    @GetMapping("/paged")
    public ResponseEntity<PagedResponse<CategoryResponse>> getAllCategoriesPaged(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        try {
            // Service direkt DTO döndürür - NO CONVERSION NEEDED - CACHED!
            Page<CategoryResponse> responsePage = categoryService.getAllCategories(pageable);

            // CUSTOM_BASE kategorisini filtrele
            List<CategoryResponse> filteredContent = responsePage.getContent().stream()
                    .filter(c -> c != null &&
                            !DatabaseConstants.CUSTOM_CATEGORY_NAME.getStringValue().equals(c.name()))
                    .toList();

            // Filtered Page oluştur
            Page<CategoryResponse> filteredPage = new org.springframework.data.domain.PageImpl<>(
                    filteredContent,
                    pageable,
                    responsePage.getTotalElements());

            return ResponseEntity.ok(PagedResponse.of(filteredPage));

        } catch (Exception e) {
            System.err.println("Error fetching paginated categories: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Elasticsearch ile kategori arama
     */
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<CategoryResponse>> searchCategories(
            @RequestParam String query,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {

        Page<CategoryResponse> page = categorySearchService.searchCategories(query, pageable);

        // PagedResponse.of() static metodunu kullan (en kolay yöntem)
        return ResponseEntity.ok(PagedResponse.of(page));
    }

    /**
     * Database ile kategori arama (Elasticsearch alternatifi)
     * 
     * Elasticsearch olmadan çalışabilir - basit isim araması için
     * GET /api/category/search/db?name=pizza&page=0&size=10
     */
    @GetMapping("/search/db")
    public ResponseEntity<PagedResponse<CategoryResponse>> searchCategoriesByName(
            @RequestParam String name,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        try {
            Page<Category> categoryPage = categoryService.searchByName(name, pageable);

            // Category -> CategoryResponse mapping
            Page<CategoryResponse> responsePage = categoryPage
                    .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getImg()));

            // Filter out CUSTOM_BASE category
            List<CategoryResponse> filteredContent = responsePage.getContent().stream()
                    .filter(c -> c.name() != null &&
                            !DatabaseConstants.CUSTOM_CATEGORY_NAME.getStringValue().equals(c.name()))
                    .toList();

            Page<CategoryResponse> filteredPage = new org.springframework.data.domain.PageImpl<>(
                    filteredContent,
                    pageable,
                    responsePage.getTotalElements());

            return ResponseEntity.ok(PagedResponse.of(filteredPage));

        } catch (Exception e) {
            System.err.println("Error searching categories by name: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Tüm kategorileri Elasticsearch'e yeniden indexle
     */
    @PostMapping("/reindex")
    public ResponseEntity<String> reindexAllCategories() {
        categorySearchService.indexAllCategories();
        return ResponseEntity.ok("All categories reindexed successfully");
    }
}
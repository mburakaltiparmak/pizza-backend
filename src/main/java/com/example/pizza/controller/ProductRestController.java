package com.example.pizza.controller;

import com.example.pizza.dto.product.CustomPizzaRequest;
import com.example.pizza.exceptions.common.ResourceNotFoundException;
import com.example.pizza.dto.paginate.PagedResponse;
import com.example.pizza.dto.product.ProductResponse;
import com.example.pizza.entity.category.Category;
import com.example.pizza.entity.product.Product;
import com.example.pizza.service.category.CategoryService;
import com.example.pizza.service.product.ProductSearchService;
import com.example.pizza.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/product")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProductRestController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final ProductSearchService productSearchService;

    private static final String CUSTOM_CATEGORY_NAME = "CUSTOM_BASE";

    // ============================================================================
    // LEGACY ENDPOINTS (CRUD & Non-Paginated)
    // ============================================================================

    /*
     * @PostMapping("/custom-pizza")
     * public ResponseEntity<ProductResponse> createCustomPizza(@RequestBody
     * CustomPizzaRequest request) {
     * try {
     * Category customCategory;
     * try {
     * customCategory = categoryService.findByName(CUSTOM_CATEGORY_NAME);
     * } catch (ResourceNotFoundException e) {
     * log.info("Category '{}' not found. Creating automatically...",
     * CUSTOM_CATEGORY_NAME);
     * Category newCategory = new Category();
     * newCategory.setName(CUSTOM_CATEGORY_NAME);
     * newCategory.setImg(
     * "https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png"
     * );
     * try {
     * customCategory = categoryService.save(newCategory, null);
     * } catch (IOException ex) {
     * throw new RuntimeException("Failed to create default category", ex);
     * }
     * }
     * 
     * Product customPizza = new Product();
     * customPizza.setName(request.getName());
     * customPizza.setRating(4.9);
     * customPizza.setStock(1);
     * customPizza.setPrice(request.getTotalPrice());
     * customPizza.setCategory(customCategory);
     * customPizza.setImg(
     * "https://res.cloudinary.com/dqjqkgpt3/image/upload/v1724010330/food-2_zwrtrh.png"
     * );
     * customPizza.setDescription(request.getCustomDetails());
     * 
     * Product savedPizza = productService.saveCustomPizza(customPizza);
     * return ResponseEntity.ok(convertToDTO(savedPizza));
     * } catch (Exception e) {
     * log.error("Custom Pizza Error: ", e);
     * return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
     * }
     * }
     */

    @GetMapping
    public ResponseEntity<List<ProductResponse>> findAll() {
        try {
            List<ProductResponse> products = productService.getAllProducts();

            List<ProductResponse> filtered = products.stream()
                    .filter(p -> p.categoryId() != null)
                    .filter(p -> !CUSTOM_CATEGORY_NAME.equals(p.categoryName()))
                    .toList();

            return ResponseEntity.ok(filtered);
        } catch (Exception e) {
            log.error("Error fetching products: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> findById(@PathVariable Long id) {
        try {
            Product product = productService.findById(id);
            return ResponseEntity.ok(convertToDTO(product));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping(consumes = { "multipart/form-data" })
    public ResponseEntity<?> save(
            @RequestParam(value = "image", required = false) MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("rating") double rating,
            @RequestParam("stock") int stock,
            @RequestParam("price") double price,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam(value = "description", required = false) String description) {
        try {
            Category category = categoryService.findById(categoryId);

            Product product = new Product();
            product.setName(name);
            product.setRating(rating);
            product.setStock(stock);
            product.setPrice(price);
            product.setCategory(category);
            product.setDescription(description);

            Product savedProduct = productService.save(product, file);
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(savedProduct));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Dosya hatası");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Kayıt hatası: " + e.getMessage());
        }
    }

    @PutMapping(value = "/{id}", consumes = { "multipart/form-data" })
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestParam(value = "image", required = false) MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("rating") double rating,
            @RequestParam("stock") int stock,
            @RequestParam("price") double price,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam(value = "description", required = false) String description) {
        try {
            Category category = categoryService.findById(categoryId);

            Product productUpdate = new Product();
            productUpdate.setName(name);
            productUpdate.setRating(rating);
            productUpdate.setStock(stock);
            productUpdate.setPrice(price);
            productUpdate.setCategory(category);
            productUpdate.setDescription(description);

            Product updatedProduct = productService.update(id, productUpdate, file);
            return ResponseEntity.ok(convertToDTO(updatedProduct));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Güncelleme hatası: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            productService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============================================================================
    // PAGINATION ENDPOINTS (DB - Cached)
    // ============================================================================

    /**
     * Get products by category (Legacy non-paginated endpoint for backward
     * compatibility)
     * 
     * @deprecated Use {@link #getProductsByCategoryPaged(Long, Pageable)} for
     *             pagination
     * 
     *             Endpoint: GET /api/product/category/{categoryId}
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductResponse>> getProductsByCategory(@PathVariable Long categoryId) {
        try {
            log.debug("Legacy endpoint: Fetching products for category ID: {}", categoryId);
            List<Product> products = productService.getProductsByCategory(categoryId);

            List<ProductResponse> response = products.stream()
                    .filter(p -> p.getCategory() != null &&
                            !CUSTOM_CATEGORY_NAME.equals(p.getCategory().getName()))
                    .map(this::convertToDTO)
                    .toList();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching products for category {}: ", categoryId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/paged")
    public ResponseEntity<PagedResponse<ProductResponse>> getAllProductsPaged(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            Page<ProductResponse> responsePage = productService.getAllProducts(pageable);

            // CUSTOM_BASE filtresi
            List<ProductResponse> filteredContent = responsePage.getContent().stream()
                    .filter(p -> p.categoryName() != null && !CUSTOM_CATEGORY_NAME.equals(p.categoryName()))
                    .toList();

            Page<ProductResponse> filteredPage = new PageImpl<>(
                    filteredContent, pageable, responsePage.getTotalElements());

            return ResponseEntity.ok(PagedResponse.of(filteredPage));
        } catch (Exception e) {
            log.error("Paged fetch error: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/paged/category/{categoryId}")
    public ResponseEntity<PagedResponse<ProductResponse>> getProductsByCategoryPaged(
            @PathVariable Long categoryId,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        try {
            Page<ProductResponse> responsePage = productService.getProductsByCategory(categoryId, pageable);
            return ResponseEntity.ok(PagedResponse.of(responsePage));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/paged/stock")
    public ResponseEntity<PagedResponse<ProductResponse>> getProductsByStockPaged(
            @RequestParam boolean inStock,
            @PageableDefault(size = 10, sort = "stock", direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            Page<ProductResponse> responsePage = productService.findByStockAvailability(inStock, pageable);
            return ResponseEntity.ok(PagedResponse.of(responsePage));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ============================================================================
    // CENTRALIZED ELASTICSEARCH ENDPOINT
    // ============================================================================

    /**
     * Merkezi Ürün Arama Endpoint'i
     * Tüm filtreler opsiyoneldir. Dolu olanlara göre dinamik sorgu oluşturulur.
     *
     * URL Örnekleri:
     * - /api/product/search?query=pizza
     * - /api/product/search?categoryId=1&minPrice=100
     * - /api/product/search?query=acılı&inStock=true&sort=price,desc
     */
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<ProductResponse>> searchProducts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {

        Page<ProductResponse> page = productSearchService.searchProductsDynamic(
                query, categoryId, minPrice, maxPrice, inStock, pageable);

        return ResponseEntity.ok(PagedResponse.of(page));
    }

    @PostMapping("/reindex")
    public ResponseEntity<String> reindexAllProducts() {
        productSearchService.indexAllProducts();
        return ResponseEntity.ok("All products reindexed successfully");
    }
    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    private ProductResponse convertToDTO(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getRating(),
                product.getStock(),
                product.getPrice(),
                product.getImg(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getDescription());
    }
}
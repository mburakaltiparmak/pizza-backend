package com.example.pizza.service.product;

import com.example.pizza.dto.product.ProductResponse;
import com.example.pizza.entity.product.Product;
import com.example.pizza.exceptions.common.FileOperationException;
import com.example.pizza.exceptions.common.ResourceNotFoundException;
import com.example.pizza.repository.CategoryRepository;
import com.example.pizza.repository.ProductRepository;
import com.example.pizza.service.logic.FileUpload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final FileUpload fileUpload;
    private final CategoryRepository categoryRepository;
    private final ProductSearchService productSearchService; // ES Service Injection

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'all'")
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::toProductResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ürün bulunamadı: ID " + id));
    }

    // ============================================================================
    // CUD OPERATIONS (Create, Update, Delete) WITH ELASTICSEARCH SYNC
    // ============================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "products", allEntries = true)
    public Product save(Product product, MultipartFile file) throws IOException {
        log.info("Saving new product: {}", product.getName());

        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Ürün adı boş olamaz");
        }
        if (product.getCategory() == null) {
            throw new IllegalArgumentException("Kategori boş olamaz");
        }

        if (file != null && !file.isEmpty()) {
            String imageUrl = fileUpload.uploadFile(file);
            product.setImg(imageUrl);
        }

        // 1. DB Save
        Product savedProduct = productRepository.save(product);

        // 2. Elasticsearch Indexing
        try {
            productSearchService.indexProduct(savedProduct);
        } catch (Exception e) {
            log.error("Elasticsearch indexing failed for product ID: {}", savedProduct.getId(), e);
            // Indexleme hatası transaction'ı rollback yapmamalı, loglayıp devam ediyoruz.
        }

        return savedProduct;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "products", allEntries = true)
    public Product update(Long id, Product product, MultipartFile file) throws IOException {
        log.info("Updating product ID: {}", id);

        Product existingProduct = findById(id);

        if (product.getName() != null && !product.getName().trim().isEmpty()) {
            existingProduct.setName(product.getName());
        }
        if (product.getDescription() != null) {
            existingProduct.setDescription(product.getDescription());
        }
        if (product.getPrice() > 0) {
            existingProduct.setPrice(product.getPrice());
        }
        if (product.getRating() >= 0 && product.getRating() <= 5) {
            existingProduct.setRating(product.getRating());
        }
        if (product.getStock() >= 0) {
            existingProduct.setStock(product.getStock());
        }
        if (product.getCategory() != null) {
            existingProduct.setCategory(product.getCategory());
        }

        if (file != null && !file.isEmpty()) {
            if (existingProduct.getImg() != null) {
                try {
                    fileUpload.deleteFile(existingProduct.getImg());
                } catch (IOException e) {
                    log.warn("Failed to delete old image: {}", e.getMessage());
                }
            }
            String newImageUrl = fileUpload.uploadFile(file);
            existingProduct.setImg(newImageUrl);
        }

        // 1. DB Update
        Product updatedProduct = productRepository.save(existingProduct);

        // 2. Elasticsearch Update
        try {
            productSearchService.indexProduct(updatedProduct);
        } catch (Exception e) {
            log.error("Elasticsearch update failed for product ID: {}", updatedProduct.getId(), e);
        }

        return updatedProduct;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "products", allEntries = true)
    public void delete(Long id) {
        log.info("Deleting product ID: {}", id);

        Product product = findById(id);

        if (product.getImg() != null) {
            try {
                fileUpload.deleteFile(product.getImg());
            } catch (IOException e) {
                throw new FileOperationException("Ürün resmi silinirken hata oluştu: " + e.getMessage());
            }
        }

        // 1. DB Delete
        productRepository.delete(product);

        // 2. Elasticsearch Delete
        try {
            productSearchService.deleteProductFromIndex(id);
        } catch (Exception e) {
            log.error("Elasticsearch delete failed for product ID: {}", id, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "products", allEntries = true)
    public Product saveCustomPizza(Product product) throws IOException {
        if (product.getName() == null) product.setName("Custom Pizza");
        if (product.getPrice() <= 0) throw new IllegalArgumentException("Fiyat 0'dan büyük olmalı");
        if (product.getCategory() == null) throw new IllegalArgumentException("Kategori gereklidir");

        return productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Kategori bulunamadı: ID " + categoryId);
        }
        return productRepository.findByCategoryId(categoryId);
    }

    // ============================================================================
    // PAGINATION METHODS
    // ============================================================================

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'page_' + #pageable.pageNumber + '_size_' + #pageable.pageSize")
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::toProductResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'category_' + #categoryId + '_page_' + #pageable.pageNumber")
    public Page<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Kategori bulunamadı");
        }
        return productRepository.findByCategoryId(categoryId, pageable).map(this::toProductResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> searchByName(String name, Pageable pageable) {
        if (name == null || name.trim().isEmpty()) return productRepository.findAll(pageable);
        return productRepository.searchByName(name, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> findByPriceRange(Double minPrice, Double maxPrice, Pageable pageable) {
        return productRepository.findByPriceRange(minPrice, maxPrice, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'instock_' + #inStock + '_page_' + #pageable.pageNumber")
    public Page<ProductResponse> findByStockAvailability(boolean inStock, Pageable pageable) {
        return productRepository.findByStockAvailability(inStock, pageable).map(this::toProductResponse);
    }

    // --- Helper Method ---
    private ProductResponse toProductResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getRating(),
                product.getStock(),
                product.getPrice(),
                product.getImg(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getDescription()
        );
    }
}
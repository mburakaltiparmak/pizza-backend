package com.example.pizza.service.category;

import com.example.pizza.dto.category.CategoryResponse;
import com.example.pizza.entity.category.Category;
import com.example.pizza.entity.product.Product;
import com.example.pizza.exceptions.common.FileOperationException;
import com.example.pizza.exceptions.common.ResourceNotFoundException;
import com.example.pizza.repository.CategoryRepository;
import com.example.pizza.service.logic.FileUploadImpl;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final FileUploadImpl fileUploadImpl;
    private final CategorySearchService categorySearchService;

    // ============================================================================
    // LEGACY METHODS (Backward Compatibility)
    // ============================================================================

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "'all'")
    public List<CategoryResponse> getAllCategories() {
        log.debug("Fetching all categories (non-paginated)");
        return categoryRepository.findAll().stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Category findById(Long id) {
        log.debug("Finding category by ID: {}", id);
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kategori bulunamadı: ID " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Category findByName(String name) {
        log.debug("Finding category by name: {}", name);
        return categoryRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Kategori bulunamadı: " + name));
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public Category save(Category category, MultipartFile file) throws IOException {
        log.info("Saving new category: {}", category.getName());

        if (file != null && !file.isEmpty()) {
            String imageUrl = fileUploadImpl.uploadFile(file);
            category.setImg(imageUrl);
        }

        // CRITICAL: Ensure createdAt is set BEFORE saving
        if (category.getCreatedAt() == null) {
            category.setCreatedAt(LocalDate.now());
        }

        Category savedCategory = categoryRepository.save(category);

        // Elasticsearch'e indexle (artık createdAt garantili)
        categorySearchService.indexCategory(savedCategory);

        log.info("Category saved successfully with ID: {}", savedCategory.getId());
        return savedCategory;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "categories", allEntries = true)
    public Category update(Long id, Category category, MultipartFile file) throws IOException {
        log.info("Updating category ID: {}", id);

        Category existingCategory = findById(id);

        if (category.getName() != null && !category.getName().trim().isEmpty()) {
            existingCategory.setName(category.getName());
        }

        if (file != null && !file.isEmpty()) {
            if (existingCategory.getImg() != null) {
                try {
                    fileUploadImpl.deleteFile(existingCategory.getImg());
                } catch (IOException e) {
                    log.warn("Eski resim silinirken hata oluştu (devam ediliyor): {}", e.getMessage());
                }
            }
            String newImageUrl = fileUploadImpl.uploadFile(file);
            existingCategory.setImg(newImageUrl);
        }

        Category updatedCategory = categoryRepository.save(existingCategory);
        categorySearchService.indexCategory(updatedCategory);
        log.info("Category updated successfully: {}", updatedCategory.getId());
        return updatedCategory;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "categories", allEntries = true)
    public void delete(Long id) {
        log.info("Deleting category ID: {}", id);

        Optional<Category> deletedCategory = categoryRepository.findById(id);
        if (deletedCategory.isEmpty()) {
            throw new ResourceNotFoundException("Kategori bulunamadı: ID " + id);
        }

        Category category = deletedCategory.get();
        if (category.getImg() != null) {
            try {
                fileUploadImpl.deleteFile(category.getImg());
            } catch (IOException e) {
                throw new FileOperationException("Kategori resmi silinirken hata oluştu: " + e.getMessage());
            }
        }
        categoryRepository.delete(category);
        categorySearchService.deleteCategoryFromIndex(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getAllProductsByCategory(Long categoryId) {
        log.debug("Fetching products for category ID: {}", categoryId);

        Category category = categoryRepository.findByIdWithProducts(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Kategori bulunamadı: " + categoryId));

        return category.getProducts();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getAllCategoriesWithProducts() {
        log.debug("Fetching all categories with products (non-paginated)");
        return categoryRepository.findAllWithProducts();
    }

    // ============================================================================
    // PAGINATION METHODS (DTO-BASED WITH CACHE)
    // ============================================================================

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "'page_' + #pageable.pageNumber + '_size_' + #pageable.pageSize + " +
            "'_sort_' + (#pageable.sort.isSorted() ? #pageable.sort.toString() : 'unsorted')")
    public Page<CategoryResponse> getAllCategories(Pageable pageable) {
        log.debug("Fetching paginated categories - page: {}, size: {}, sort: {}",
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort());

        Page<Category> categoryPage = categoryRepository.findAll(pageable);
        return categoryPage.map(this::toCategoryResponse);
    }

    /**
     * Kategorileri products ile birlikte sayfalı getir
     *
     * NOT: Bu method cache edilmiyor çünkü products eager loading yapıldığı için
     * memory kullanımı yüksek olabilir.
     *
     * @param pageable Pagination parametreleri
     * @return Page<Category> - Products dahil
     */
    @Override
    @Transactional(readOnly = true)
    public Page<Category> getAllCategoriesWithProducts(Pageable pageable) {
        log.debug("Fetching paginated categories with products - page: {}, size: {}",
                pageable.getPageNumber(),
                pageable.getPageSize());

        if (pageable.getPageSize() > 20) {
            log.warn("⚠️ Large page size ({}) requested for categories with products. " +
                    "Consider using smaller page sizes or loading products separately.",
                    pageable.getPageSize());
        }

        return categoryRepository.findAllWithProducts(pageable);
    }

    /**
     * İsme göre kategori arama (sayfalı)
     *
     * Database-level search with case-insensitive partial matching.
     * Much more performant than in-memory filtering.
     *
     * @param name     Aranacak kategori adı (partial, case-insensitive)
     * @param pageable Pagination parametreleri
     * @return Paginated search results
     */
    @Override
    @Transactional(readOnly = true)
    public Page<Category> searchByName(String name, Pageable pageable) {
        log.debug("Searching categories by name: '{}' - page: {}, size: {}",
                name,
                pageable.getPageNumber(),
                pageable.getPageSize());

        if (name == null || name.trim().isEmpty()) {
            log.warn("Empty search term provided, returning all categories");
            return categoryRepository.findAll(pageable);
        }

        // ✅ FIXED: Now using repository method instead of in-memory filtering
        return categoryRepository.searchByName(name.trim(), pageable);
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    private CategoryResponse toCategoryResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getImg());
    }
}
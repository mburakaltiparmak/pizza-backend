package com.example.pizza.service.category;

import com.example.pizza.dto.category.CategoryResponse;
import com.example.pizza.entity.category.Category;
import com.example.pizza.repository.CategoryRepository;
import com.example.pizza.entity.category.CategoryDocument;
import com.example.pizza.repository.search.CategorySearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategorySearchService {

    private final CategorySearchRepository categorySearchRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public void indexCategory(Category category) {
        // LocalDateTime -> LocalDate conversion
        LocalDate createdAtDate = category.getCreatedAt();

        CategoryDocument document = CategoryDocument.builder()
                .id(category.getId())
                .name(category.getName())
                .img(category.getImg())
                .createdAt(createdAtDate)  // LocalDate
                .build();

        categorySearchRepository.save(document);
        log.info("Category indexed: ID={}, Name='{}', CreatedAt={}",
                category.getId(), category.getName(), createdAtDate);
    }

    @Transactional
    public void indexAllCategories() {
        log.info("Starting bulk category indexing...");
        long startTime = System.currentTimeMillis();
        long count = 0;

        for (Category category : categoryRepository.findAll()) {
            indexCategory(category);
            count++;
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Bulk indexing completed: {} categories in {}ms", count, duration);
    }

    @Transactional(readOnly = true)
    public Page<CategoryResponse> searchCategories(String query, Pageable pageable) {
        log.debug("Searching categories: query='{}', page={}, size={}",
                query, pageable.getPageNumber(), pageable.getPageSize());

        Page<CategoryDocument> documents = categorySearchRepository
                .findByNameContainingIgnoreCase(query, pageable);

        log.debug("Found {} categories", documents.getTotalElements());

        return documents.map(this::toResponse);
    }

    public void deleteCategoryFromIndex(Long categoryId) {
        categorySearchRepository.deleteById(categoryId);
        log.info("Category deleted from index: {}", categoryId);
    }

    private CategoryResponse toResponse(CategoryDocument document) {
        return new CategoryResponse(
                document.getId(),
                document.getName(),
                document.getImg()
        );
    }
}
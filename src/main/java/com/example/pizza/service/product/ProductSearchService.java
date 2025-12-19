package com.example.pizza.service.product;

import com.example.pizza.dto.product.ProductResponse;
import com.example.pizza.entity.product.Product;
import com.example.pizza.repository.ProductRepository;
import com.example.pizza.entity.product.ProductDocument;
import com.example.pizza.repository.search.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchService {

    private final ProductSearchRepository productSearchRepository;
    private final ProductRepository productRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Transactional
    public void indexProduct(Product product) {
        try {
            LocalDate createdAtDate = (product.getCreatedAt() != null)
                    ? product.getCreatedAt().toLocalDate()
                    : LocalDate.now();

            ProductDocument document = ProductDocument.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .description(product.getDescription())
                    .price(product.getPrice())
                    .stock(product.getStock())
                    .rating(product.getRating())
                    .img(product.getImg())
                    .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                    .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                    .createdAt(createdAtDate)
                    .build();

            productSearchRepository.save(document);
            log.debug("Product indexed: ID={}", product.getId());
        } catch (Exception e) {
            log.error("Failed to index product ID: {}", product.getId(), e);
        }
    }

    @Transactional
    public void deleteProductFromIndex(Long productId) {
        try {
            productSearchRepository.deleteById(productId);
            log.info("Product deleted from index: ID={}", productId);
        } catch (Exception e) {
            log.error("Failed to delete product from index ID: {}", productId, e);
        }
    }

    @Transactional
    public void indexAllProducts() {
        log.info("Starting bulk product indexing...");
        List<Product> products = productRepository.findAll();
        int count = 0;
        for (Product product : products) {
            indexProduct(product);
            count++;
        }
        log.info("Bulk indexing completed. Total: {}", count);
    }

    // ============================================================================
    // SEARCH METHODS
    // ============================================================================

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProductsDynamic(
            String query, Long categoryId, Double minPrice, Double maxPrice, Boolean inStock, Pageable pageable) {

        Criteria criteria = new Criteria();

        // 1. Query (İsim veya Açıklama)
        if (query != null && !query.trim().isEmpty()) {
            criteria = criteria.subCriteria(
                    new Criteria("name").contains(query)
                            .or("description").contains(query));
        }

        // 2. Kategori
        if (categoryId != null) {
            criteria = criteria.and("categoryId").is(categoryId);
        }

        // 3. Fiyat
        if (minPrice != null || maxPrice != null) {
            if (minPrice != null && maxPrice != null) {
                criteria = criteria.and("price").between(minPrice, maxPrice);
            } else if (minPrice != null) {
                criteria = criteria.and("price").greaterThanEqual(minPrice);
            } else {
                criteria = criteria.and("price").lessThanEqual(maxPrice);
            }
        }

        // 4. Stok
        if (inStock != null && inStock) {
            criteria = criteria.and("stock").greaterThan(0);
        }

        CriteriaQuery criteriaQuery = new CriteriaQuery(criteria).setPageable(pageable);
        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(criteriaQuery, ProductDocument.class);

        List<ProductResponse> responses = searchHits.stream()
                .map(SearchHit::getContent)
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, searchHits.getTotalHits());
    }

    // ============================================================================
    // PHASE 6: AUTOCOMPLETE / SUGGESTIONS
    // ============================================================================

    /**
     * Autocomplete için ürün ismi önerileri
     *
     * Best Practice: Wildcard query kullanarak hem başlangıç hem de kelime içi
     * eşleşme
     *
     * Örnekler:
     * - "piz" → ["Pizza Margherita", "Position Pizza", ...]
     * - "chck" → ["Chicken Pizza", "Check Pizza", ...]
     * - "term" → ["Terminal Pizza", ...]
     *
     * @param partialQuery Kısmi kelime (ör: "piz", "chck")
     * @param limit        Max öneri sayısı (default: 5)
     * @return Öneri listesi
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "search-suggestions", key = "#partialQuery + '_' + #limit")
    public List<String> getSuggestions(String partialQuery, int limit) {
        if (partialQuery == null || partialQuery.trim().length() < 2) {
            return List.of(); // Minimum 2 karakter gerekli
        }

        // Native Elasticsearch wildcard query (Best Practice)
        // *query* pattern: kelime başı, ortası veya sonunda eşleşir
        String wildcardPattern = "*" + partialQuery.toLowerCase() + "*";

        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(q -> q
                        .wildcard(w -> w
                                .field("name")
                                .value(wildcardPattern)
                                .caseInsensitive(true)))
                .withMaxResults(limit * 2) // Distinct için ekstra results
                .build();

        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(
                searchQuery,
                ProductDocument.class);

        return searchHits.stream()
                .map(hit -> hit.getContent().getName())
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Best Practice: Elasticsearch native fuzzy query with AUTO fuzziness
     *
     * Bu metod GERÇEK yazım hatalarını tolere eder:
     * - "pizaa" → "pizza" (1 harf fazla)
     * - "chiken" → "chicken" (1 harf yanlış)
     * - "margarta" → "margarita" (1 harf yanlış)
     *
     * Fuzziness AUTO:
     * - 1-2 karakter: fuzziness 0 (tam eşleşme)
     * - 3-5 karakter: fuzziness 1 (1 harf hata)
     * - 6+ karakter: fuzziness 2 (2 harf hata)
     *
     * @param query Aranacak kelime (yazım hatası olabilir)
     * @param limit Max öneri sayısı
     * @return Fuzzy eşleşen ürün isimleri
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "fuzzy-suggestions", key = "#query + '_' + #limit")
    public List<String> getFuzzySuggestions(String query, int limit) {
        if (query == null || query.trim().length() < 3) {
            return List.of(); // Minimum 3 karakter gerekli (fuzzy için)
        }

        // Hybrid approach: Combine partial matching + fuzzy (typo tolerance)
        // Strategy:
        // 1. match_phrase_prefix: handles "chck" → "chicken" (partial token match)
        // 2. fuzzy: handles "pizaa" → "pizza" (typo correction)

        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> b
                                .should(s -> s
                                        .matchPhrasePrefix(m -> m
                                                .field("name")
                                                .query(query.toLowerCase())
                                                .maxExpansions(10)))
                                .should(s -> s
                                        .fuzzy(f -> f
                                                .field("name")
                                                .value(query.toLowerCase())
                                                .fuzziness("AUTO")
                                                .prefixLength(0)))
                                .minimumShouldMatch("1")))
                .withMaxResults(limit * 2)
                .build();

        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(
                searchQuery,
                ProductDocument.class);

        return searchHits.stream()
                .map(hit -> hit.getContent().getName())
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }

    private ProductResponse toResponse(ProductDocument document) {
        return new ProductResponse(
                document.getId(),
                document.getName(),
                document.getRating(),
                document.getStock(),
                document.getPrice(),
                document.getImg(),
                document.getCategoryId(),
                document.getCategoryName(),
                document.getDescription());
    }
}
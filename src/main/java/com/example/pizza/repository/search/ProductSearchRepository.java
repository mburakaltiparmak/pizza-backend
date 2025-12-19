package com.example.pizza.repository.search;

import com.example.pizza.entity.product.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, Long> {

    // 1. İsim VEYA Açıklama içinde arama (Global Search)
    // Case-insensitive (Büyük/küçük harf duyarsız)
    Page<ProductDocument> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String description, Pageable pageable);

    // 2. Sadece İsim içinde arama
    Page<ProductDocument> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // 3. Sadece Açıklama içinde arama
    Page<ProductDocument> findByDescriptionContainingIgnoreCase(String description, Pageable pageable);

    // 4. Kategoriye göre tam eşleşme
    Page<ProductDocument> findByCategoryId(Long categoryId, Pageable pageable);

    // 5. Fiyat Aralığı
    Page<ProductDocument> findByPriceBetween(Double minPrice, Double maxPrice, Pageable pageable);

    // 6. Stokta olanlar (Stock > X)
    Page<ProductDocument> findByStockGreaterThan(Integer stock, Pageable pageable);

    // 7. Gelişmiş Arama: (İsim VEYA Açıklama) VE Kategori
    // Metod ismi çok karmaşık olacağı için @Query (Elasticsearch JSON DSL) kullanmak Best Practice'tir.
    // ?0 -> query parametresi, ?1 -> categoryId parametresi
    @Query("{\"bool\": {\"must\": [{\"match\": {\"categoryId\": \"?1\"}}, {\"bool\": {\"should\": [{\"wildcard\": {\"name\": \"*?0*\"}}, {\"wildcard\": {\"description\": \"*?0*\"}}]}}]}}")
    Page<ProductDocument> searchByQueryAndCategory(String query, Long categoryId, Pageable pageable);
}
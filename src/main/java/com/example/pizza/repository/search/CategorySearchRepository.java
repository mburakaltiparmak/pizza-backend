package com.example.pizza.repository.search;

import com.example.pizza.entity.category.CategoryDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategorySearchRepository extends ElasticsearchRepository<CategoryDocument, Long> {

    Page<CategoryDocument> findByNameContaining(String name, Pageable pageable);

    Page<CategoryDocument> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
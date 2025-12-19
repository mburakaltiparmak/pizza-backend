package com.example.pizza.repository.search;

import com.example.pizza.entity.order.OrderDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderSearchRepository extends ElasticsearchRepository<OrderDocument, Long> {
}

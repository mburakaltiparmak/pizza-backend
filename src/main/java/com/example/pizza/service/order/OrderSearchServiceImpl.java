package com.example.pizza.service.order;

import com.example.pizza.constants.order.OrderStatus;
import com.example.pizza.entity.order.Order;
import com.example.pizza.entity.order.OrderDocument;
import com.example.pizza.repository.OrderRepository;
import com.example.pizza.repository.search.OrderSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSearchServiceImpl implements OrderSearchService {

    private final OrderSearchRepository orderSearchRepository;
    private final OrderRepository orderRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Async
    @Override
    public void indexOrder(Order order) {
        try {
            OrderDocument document = OrderDocument.builder()
                    .id(order.getId())
                    .userEmail(order.getUser() != null ? order.getUser().getEmail() : "guest")
                    .orderStatus(order.getOrderStatus())
                    .totalAmount(order.getTotalAmount())
                    .orderDate(order.getOrderDate().toLocalDate())
                    .build();

            orderSearchRepository.save(document);
            log.info("Order indexed successfully: {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to index order: {}", order.getId(), e);
        }
    }

    @Async
    @Override
    public void deleteOrderFromIndex(Long id) {
        try {
            orderSearchRepository.deleteById(id);
            log.info("Order deleted from index: {}", id);
        } catch (Exception e) {
            log.error("Failed to delete order from index: {}", id, e);
        }
    }

    @Async
    @Transactional(readOnly = true)
    @Override
    public void indexAllOrders() {
        log.info("Starting full reindex of orders...");
        List<Order> orders = orderRepository.findAll();

        List<OrderDocument> documents = orders.stream()
                .map(order -> OrderDocument.builder()
                        .id(order.getId())
                        .userEmail(order.getUser() != null ? order.getUser().getEmail() : "guest")
                        .orderStatus(order.getOrderStatus())
                        .totalAmount(order.getTotalAmount())
                        .orderDate(order.getOrderDate().toLocalDate())
                        .build())
                .collect(Collectors.toList());

        orderSearchRepository.saveAll(documents);
        log.info("Finished reindexing {} orders", documents.size());
    }

    @Override
    public Page<OrderDocument> searchOrdersDynamic(String userEmail, OrderStatus status, Double minPrice,
            Double maxPrice, Pageable pageable) {
        Criteria criteria = new Criteria();

        if (userEmail != null && !userEmail.isEmpty()) {
            criteria = criteria.and("userEmail").contains(userEmail);
        }

        if (status != null) {
            criteria = criteria.and("orderStatus").is(status);
        }

        if (minPrice != null && maxPrice != null) {
            criteria = criteria.and("totalAmount").between(minPrice, maxPrice);
        } else if (minPrice != null) {
            criteria = criteria.and("totalAmount").greaterThanEqual(minPrice);
        } else if (maxPrice != null) {
            criteria = criteria.and("totalAmount").lessThanEqual(maxPrice);
        }

        CriteriaQuery query = new CriteriaQuery(criteria).setPageable(pageable);
        SearchHits<OrderDocument> searchHits = elasticsearchOperations.search(query, OrderDocument.class);

        List<OrderDocument> content = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, searchHits.getTotalHits());
    }
}

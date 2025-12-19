package com.example.pizza.service.order;

import com.example.pizza.constants.order.OrderStatus;
import com.example.pizza.entity.order.Order;
import com.example.pizza.entity.order.OrderDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderSearchService {
    void indexOrder(Order order);

    void deleteOrderFromIndex(Long id);

    void indexAllOrders();

    Page<OrderDocument> searchOrdersDynamic(String userEmail, OrderStatus status, Double minPrice, Double maxPrice,
            Pageable pageable);
}

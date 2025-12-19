package com.example.pizza.service.order;

import com.example.pizza.constants.order.OrderStatus;
import com.example.pizza.dto.order.OrderCreateRequest;
import com.example.pizza.entity.order.Order;
import com.example.pizza.entity.logic.Payment;
import com.example.pizza.entity.user.User;
import com.example.pizza.exceptions.base.ValidationException;
import com.example.pizza.exceptions.common.ResourceNotFoundException;
import com.example.pizza.exceptions.order.InsufficientStockException;
import com.example.pizza.exceptions.order.OrderCreationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderService {

    // ============================================================================
    // READ OPERATIONS (Non-paginated)
    // ============================================================================
    List<Order> getAllOrders();
    List<Order> getOrdersByUser(Long userId);
    List<Order> getOrdersByStatus(OrderStatus status);
    Order getOrderById(Long id);

    // ============================================================================
    // CREATE OPERATION
    // ============================================================================
    Order createOrder(OrderCreateRequest request, User user);

    // ============================================================================
    // UPDATE OPERATIONS
    // ============================================================================
    Order updateOrderStatus(Long orderId, OrderStatus newStatus);
    void cancelOrder(Long orderId);

    // ============================================================================
    // PAYMENT OPERATIONS
    // ============================================================================
    Payment processCashPayment(Long orderId);

    // ============================================================================
    // READ OPERATIONS (Paginated)
    // ============================================================================
    Page<Order> getAllOrders(Pageable pageable);
    Page<Order> getOrdersByUser(Long userId, Pageable pageable);
    Page<Order> getOrdersByStatus(OrderStatus status, Pageable pageable);
    Page<Order> getOrdersBetweenDates(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    Page<Order> getOrdersByPriceRange(Double minPrice, Double maxPrice, Pageable pageable);
}
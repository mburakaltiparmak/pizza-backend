package com.example.pizza.controller;

import com.example.pizza.constants.order.OrderStatus;
import com.example.pizza.dto.order.OrderCreateRequest;
import com.example.pizza.dto.order.OrderResponse;
import com.example.pizza.dto.order.OrderStatusUpdateRequest;
import com.example.pizza.dto.order.StockErrorResponse;
import com.example.pizza.dto.paginate.PagedResponse;
import com.example.pizza.entity.logic.Payment;
import com.example.pizza.entity.order.Order;
import com.example.pizza.entity.user.User;
import com.example.pizza.logic.mapper.OrderMapper;
import com.example.pizza.dto.exceptions.ApiError;
import com.example.pizza.exceptions.order.InsufficientStockException;
import com.example.pizza.exceptions.order.OrderCreationException;
import com.example.pizza.exceptions.common.ResourceNotFoundException;
import com.example.pizza.exceptions.base.ValidationException;
import com.example.pizza.service.order.OrderService;
import com.example.pizza.service.order.OrderSearchService;
import com.example.pizza.entity.order.OrderDocument;
import com.example.pizza.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.example.pizza.dto.exceptions.ApiResponse;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderRestController {

        private final OrderService orderService;
        private final UserService userService;
        private final OrderMapper orderMapper;
        private final OrderSearchService orderSearchService;

        // ============================================================================
        // CREATE ORDER - SINGLE FORMAT
        // ============================================================================

        /**
         * Create Order
         *
         * Request Format:
         * {
         * "items": [
         * {"productId": 1, "quantity": 2}
         * ],
         * "addressId": 3, // For logged-in users with saved address
         * "newAddress": { // For guests or new address
         * "fullAddress": "...",
         * "city": "Istanbul",
         * "district": "Kadikoy",
         * "postalCode": "34710",
         * "phoneNumber": "05551234567",
         * "recipientName": "John Doe",
         * "addressTitle": "Home",
         * "email": "guest@example.com" // Required for guests
         * },
         * "paymentMethod": "CASH", // CASH, CREDIT_CARD, ONLINE_CREDIT_CARD, GIFT_CARD
         * "notes": "Extra cheese" // Optional
         * }
         *
         * Response: OrderResponse with order details
         *
         * Status Codes:
         * - 200: Order created successfully
         * - 400: Validation error
         * - 409: Insufficient stock
         * - 500: Internal server error
         */
        @PostMapping
        public ResponseEntity<?> createOrder(@Valid @RequestBody OrderCreateRequest request) {
                try {
                        String email = SecurityContextHolder.getContext().getAuthentication().getName();
                        User user = null;

                        // Get user if authenticated
                        if (email != null && !email.equals("anonymousUser")) {
                                user = userService.getUserByEmailWithAddresses(email);
                                log.debug("Creating order for user: {}", email);
                        } else {
                                log.debug("Creating guest order");
                        }

                        // Create order
                        Order order = orderService.createOrder(request, user);

                        // Map to response DTO
                        OrderResponse response = orderMapper.toOrderResponse(order);

                        log.info("Order created successfully: ID={}, Total={}, User={}",
                                        order.getId(),
                                        order.getTotalAmount(),
                                        user != null ? user.getEmail() : "guest");

                        return ResponseEntity.ok(response);

                } catch (ValidationException e) {
                        log.warn("Validation error: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(new ApiError(e.getMessage()));

                } catch (InsufficientStockException e) {
                        log.warn("Insufficient stock: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(new StockErrorResponse(
                                                        e.getMessage(),
                                                        e.getProductName(),
                                                        e.getAvailableStock(),
                                                        e.getRequestedQuantity(),
                                                        LocalDateTime.now()));

                } catch (ResourceNotFoundException e) {
                        log.warn("Resource not found: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(new ApiError(e.getMessage()));

                } catch (OrderCreationException e) {
                        log.error("Order creation failed", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new ApiError("Sipariş oluşturulamadı. Lütfen tekrar deneyin."));

                } catch (Exception e) {
                        log.error("Unexpected error creating order", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(new ApiError("Beklenmeyen bir hata oluştu: " + e.getMessage()));
                }
        }

        // ============================================================================
        // READ OPERATIONS
        // ============================================================================

        @GetMapping
        @PreAuthorize("hasRole('ADMIN') or hasRole('PERSONAL')")
        public ResponseEntity<List<OrderResponse>> getAllOrders() {
                log.debug("Getting all orders");
                List<Order> orders = orderService.getAllOrders();
                List<OrderResponse> response = orders.stream()
                                .map(orderMapper::toOrderResponse)
                                .collect(Collectors.toList());
                return ResponseEntity.ok(response);
        }

        @GetMapping("/status/{status}")
        @PreAuthorize("hasRole('ADMIN') or hasRole('PERSONAL')")
        public ResponseEntity<List<OrderResponse>> getOrdersByStatus(@PathVariable OrderStatus status) {
                log.debug("Getting orders with status: {}", status);
                List<Order> orders = orderService.getOrdersByStatus(status);
                List<OrderResponse> response = orders.stream()
                                .map(orderMapper::toOrderResponse)
                                .collect(Collectors.toList());
                return ResponseEntity.ok(response);
        }

        @GetMapping("/my-orders")
        public ResponseEntity<List<OrderResponse>> getMyOrders(@AuthenticationPrincipal UserDetails userDetails) {
                log.debug("Getting orders for user: {}", userDetails.getUsername());
                Long userId = userService.getUserByEmail(userDetails.getUsername()).getId();
                List<Order> orders = orderService.getOrdersByUser(userId);
                List<OrderResponse> response = orders.stream()
                                .map(orderMapper::toOrderResponse)
                                .collect(Collectors.toList());
                return ResponseEntity.ok(response);
        }

        @GetMapping("/{id}")
        public ResponseEntity<?> getOrderById(
                        @PathVariable Long id,
                        @AuthenticationPrincipal UserDetails userDetails) {

                log.debug("Getting order ID: {} for user: {}", id, userDetails.getUsername());

                try {
                        Order order = orderService.getOrderById(id);

                        // Check authorization
                        boolean isAdminOrPersonal = userDetails.getAuthorities().stream()
                                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                                                        a.getAuthority().equals("ROLE_PERSONAL"));

                        boolean isOrderOwner = order.getUser() != null &&
                                        order.getUser().getEmail().equals(userDetails.getUsername());

                        if (!isAdminOrPersonal && !isOrderOwner) {
                                log.warn("Unauthorized access attempt to order {} by user {}",
                                                id, userDetails.getUsername());
                                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                .body(new ApiError("Bu siparişe erişim yetkiniz yok"));
                        }

                        OrderResponse response = orderMapper.toOrderResponse(order);
                        return ResponseEntity.ok(response);

                } catch (ResourceNotFoundException e) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(new ApiError(e.getMessage()));
                }
        }

        // ============================================================================
        // UPDATE OPERATIONS
        // ============================================================================

        @PatchMapping("/{id}/status")
        @PreAuthorize("hasRole('ADMIN') or hasRole('PERSONAL')")
        public ResponseEntity<?> updateOrderStatus(
                        @PathVariable Long id,
                        @RequestParam OrderStatus status) {

                log.info("Updating order {} status to {}", id, status);

                try {
                        Order updatedOrder = orderService.updateOrderStatus(id, status);
                        OrderResponse response = orderMapper.toOrderResponse(updatedOrder);
                        return ResponseEntity.ok(response);

                } catch (ResourceNotFoundException e) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(new ApiError(e.getMessage()));

                } catch (IllegalStateException e) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(new ApiError(e.getMessage()));
                }
        }

        @PutMapping("/{id}/status")
        @PreAuthorize("hasRole('ADMIN') or hasRole('PERSONAL')")
        public ResponseEntity<?> updateOrderStatusPut(
                        @PathVariable Long id,
                        @Valid @RequestBody OrderStatusUpdateRequest request) {

                Order updatedOrder = orderService.updateOrderStatus(id, request.getOrderStatus());
                OrderResponse response = orderMapper.toOrderResponse(updatedOrder);
                return ResponseEntity.ok(response);
        }

        @PostMapping("/{id}/cancel")
        public ResponseEntity<?> cancelOrder(
                        @PathVariable Long id,
                        @AuthenticationPrincipal UserDetails userDetails) {

                log.info("Cancelling order {} by user {}", id, userDetails.getUsername());

                try {
                        Order order = orderService.getOrderById(id);

                        // Check authorization
                        boolean isAdminOrPersonal = userDetails.getAuthorities().stream()
                                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                                                        a.getAuthority().equals("ROLE_PERSONAL"));

                        boolean isOrderOwner = order.getUser() != null &&
                                        order.getUser().getEmail().equals(userDetails.getUsername());

                        if (!isAdminOrPersonal && !isOrderOwner) {
                                log.warn("Unauthorized cancellation attempt for order {} by user {}",
                                                id, userDetails.getUsername());
                                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                                .body(new ApiError("Bu siparişi iptal etme yetkiniz yok"));
                        }

                        orderService.cancelOrder(id);
                        return ResponseEntity.ok(new ApiResponse(true, "Sipariş başarıyla iptal edildi"));

                } catch (ResourceNotFoundException e) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(new ApiError(e.getMessage()));

                } catch (IllegalStateException e) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(new ApiError(e.getMessage()));
                }
        }

        // ============================================================================
        // PAGINATION ENDPOINTS
        // ============================================================================

        /**
         * Get all orders paginated (Admin/Personal only)
         *
         * Query params:
         * - page: page number (default: 0)
         * - size: page size (default: 20)
         * - sort: sort field and direction (default: orderDate,desc)
         *
         * Example: GET /api/orders/admin/paged?page=0&size=10&sort=orderDate,desc
         */
        @GetMapping("/admin/paged")
        @PreAuthorize("hasAnyRole('ADMIN', 'PERSONAL')")
        public ResponseEntity<PagedResponse<OrderResponse>> getAllOrdersPaged(
                        @PageableDefault(size = 20, sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable) {

                log.debug("Getting paginated orders - page: {}, size: {}",
                                pageable.getPageNumber(), pageable.getPageSize());

                Page<Order> ordersPage = orderService.getAllOrders(pageable);

                List<OrderResponse> content = ordersPage.getContent().stream()
                                .map(orderMapper::toOrderResponse)
                                .collect(Collectors.toList());

                PagedResponse<OrderResponse> response = new PagedResponse<>(
                                content,
                                ordersPage.getNumber(),
                                ordersPage.getSize(),
                                ordersPage.getTotalElements(),
                                ordersPage.getTotalPages(),
                                ordersPage.isLast());

                return ResponseEntity.ok(response);
        }

        /**
         * Get orders by status paginated (Admin/Personal only)
         */
        @GetMapping("/admin/paged/status/{status}")
        @PreAuthorize("hasAnyRole('ADMIN', 'PERSONAL')")
        public ResponseEntity<PagedResponse<OrderResponse>> getOrdersByStatusPaged(
                        @PathVariable OrderStatus status,
                        @PageableDefault(size = 20, sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable) {

                log.debug("Getting paginated orders by status: {} - page: {}, size: {}",
                                status, pageable.getPageNumber(), pageable.getPageSize());

                Page<Order> ordersPage = orderService.getOrdersByStatus(status, pageable);

                List<OrderResponse> content = ordersPage.getContent().stream()
                                .map(orderMapper::toOrderResponse)
                                .collect(Collectors.toList());

                PagedResponse<OrderResponse> response = new PagedResponse<>(
                                content,
                                ordersPage.getNumber(),
                                ordersPage.getSize(),
                                ordersPage.getTotalElements(),
                                ordersPage.getTotalPages(),
                                ordersPage.isLast());

                return ResponseEntity.ok(response);
        }

        // ============================================================================
        // ELASTICSEARCH ENDPOINTS
        // ============================================================================

        @PostMapping("/admin/reindex")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<String> reindexAllOrders() {
                orderSearchService.indexAllOrders();
                return ResponseEntity.ok("Siparişler yeniden indeksleniyor...");
        }

        @GetMapping("/admin/search")
        @PreAuthorize("hasAnyRole('ADMIN', 'PERSONAL')")
        public ResponseEntity<PagedResponse<OrderDocument>> searchOrders(
                        @RequestParam(required = false) String userEmail,
                        @RequestParam(required = false) OrderStatus status,
                        @RequestParam(required = false) Double minPrice,
                        @RequestParam(required = false) Double maxPrice,
                        @PageableDefault(size = 20, sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable) {

                Page<OrderDocument> page = orderSearchService.searchOrdersDynamic(userEmail, status, minPrice, maxPrice,
                                pageable);

                return ResponseEntity.ok(PagedResponse.of(page));
        }
}

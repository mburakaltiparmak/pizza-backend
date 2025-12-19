package com.example.pizza.service.order;

import com.example.pizza.constants.order.OrderStatus;
import com.example.pizza.constants.order.PaymentMethod;
import com.example.pizza.constants.order.PaymentStatus;
import com.example.pizza.dto.address.DeliveryAddressRequest;
import com.example.pizza.dto.order.OrderCreateRequest;
import com.example.pizza.dto.order.OrderItemRequest;
import com.example.pizza.entity.logic.Payment;
import com.example.pizza.entity.order.Order;
import com.example.pizza.entity.order.OrderItem;
import com.example.pizza.entity.product.Product;
import com.example.pizza.entity.user.User;
import com.example.pizza.entity.user.UserAddress;
import com.example.pizza.exceptions.order.InsufficientStockException;
import com.example.pizza.exceptions.order.OrderCreationException;
import com.example.pizza.exceptions.common.ResourceNotFoundException;
import com.example.pizza.repository.OrderRepository;
import com.example.pizza.repository.ProductRepository;
import com.example.pizza.service.logic.EmailService;
import com.example.pizza.service.user.UserService;
import com.example.pizza.logic.validator.OrderValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserService userService;
    private final EmailService emailService;
    private final OrderValidator orderValidator;
    private final OrderSearchService orderSearchService;

    // ============================================================================
    // LEGACY READ OPERATIONS (Backward Compatibility)
    // ============================================================================

    @Override
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        log.debug("Fetching all orders with details");
        return orderRepository.findAllWithDetails();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUser(Long userId) {
        User user = userService.getUserById(userId);
        log.debug("Fetching orders for user ID: {}", userId);
        return orderRepository.findByUserWithDetails(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByStatus(OrderStatus status) {
        log.debug("Fetching orders with status: {}", status);
        return orderRepository.findByOrderStatusWithDetails(status);
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        log.debug("Fetching order with ID: {}", id);
        return orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş bulunamadı: ID " + id));
    }

    // ============================================================================
    // CREATE OPERATION - REFACTORED (New DTO support)
    // ============================================================================
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public Order createOrder(OrderCreateRequest request, User user) {
        log.info("Creating order for {}", user != null ? user.getEmail() : "guest");

        try {
            // 1. Validate request
            orderValidator.validateOrderRequest(request, user);

            // 2. Build order entity
            Order order = buildOrder(request, user);

            // 3. Resolve delivery address
            resolveDeliveryAddress(order, request, user);

            // 4. Process order items with stock validation
            processOrderItems(order, request.getItems());

            // 5. Create payment (will be saved via cascade)
            Payment payment = createPayment(order, request.getPaymentMethod());
            order.setPayment(payment);

            // 6. Save order (cascade saves payment + items)
            Order savedOrder = orderRepository.save(order);

            // 7. Send confirmation email asynchronously
            scheduleOrderConfirmationEmail(savedOrder);

            // 8. Index to Elasticsearch
            orderSearchService.indexOrder(savedOrder);

            log.info("Order created successfully: ID={}, Total={}, Items={}",
                    savedOrder.getId(),
                    savedOrder.getTotalAmount(),
                    savedOrder.getItems().size());

            return savedOrder;

        } catch (IllegalArgumentException | ResourceNotFoundException | InsufficientStockException e) {
            log.warn("Order creation validation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating order", e);
            throw new OrderCreationException("Sipariş oluşturulamadı", e);
        }
    }

    // ============================================================================
    // NEW DTO HELPER METHODS
    // ============================================================================

    private Order buildOrder(OrderCreateRequest request, User user) {
        Order order = new Order();
        order.setUser(user);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());
        order.setNotes(request.getNotes());
        order.setItems(new ArrayList<>());
        return order;
    }

    private void resolveDeliveryAddress(Order order, OrderCreateRequest request, User user) {
        if (request.getAddressId() != null) {
            // Use saved address
            User freshUser = userService.getUserByEmailWithAddresses(user.getEmail());
            UserAddress selectedAddress = freshUser.getAddresses().stream()
                    .filter(addr -> addr.getId() != null && addr.getId().equals(request.getAddressId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Adres bulunamadı: ID " + request.getAddressId()));

            order.setDeliveryAddress(selectedAddress);

        } else if (request.getNewAddress() != null) {
            UserAddress newAddress = mapToUserAddress(request.getNewAddress());
            order.setDeliveryAddress(newAddress);
        } else {
            throw new IllegalArgumentException("Teslimat adresi gereklidir");
        }
    }

    private void processOrderItems(Order order, List<OrderItemRequest> itemRequests) {
        List<Long> productIds = itemRequests.stream()
                .map(OrderItemRequest::getProductId)
                .distinct()
                .toList();

        if (productIds.isEmpty()) {
            throw new IllegalArgumentException("Geçerli ürün ID'leri bulunamadı");
        }

        List<Product> products = productRepository.findByIdsWithLock(productIds);

        if (products.size() != productIds.size()) {
            throw new ResourceNotFoundException("Bazı ürünler bulunamadı");
        }

        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : itemRequests) {
            Long productId = itemRequest.getProductId();
            Product product = productMap.get(productId);

            if (product == null) {
                throw new ResourceNotFoundException("Ürün bulunamadı: ID " + productId);
            }

            if (itemRequest.getQuantity() <= 0) {
                throw new IllegalArgumentException("Geçersiz miktar: " + itemRequest.getQuantity());
            }

            if (product.getStock() < itemRequest.getQuantity()) {
                throw new InsufficientStockException(
                        String.format("Yetersiz stok: %s (Mevcut: %d, İstenen: %d)",
                                product.getName(),
                                product.getStock(),
                                itemRequest.getQuantity()));
            }

            product.setStock(product.getStock() - itemRequest.getQuantity());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPrice(product.getPrice());

            order.getItems().add(orderItem);

            BigDecimal itemTotal = BigDecimal.valueOf(product.getPrice())
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);
        }

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Sipariş tutarı 0'dan büyük olmalıdır");
        }

        order.setTotalAmount(totalAmount.doubleValue());
    }

    private UserAddress mapToUserAddress(DeliveryAddressRequest dto) {
        UserAddress address = new UserAddress();
        address.setFullAddress(dto.getFullAddress());
        address.setCity(dto.getCity());
        address.setDistrict(dto.getDistrict());
        address.setPostalCode(dto.getPostalCode());
        address.setPhoneNumber(dto.getPhoneNumber());
        address.setRecipientName(dto.getRecipientName());
        address.setAddressTitle(dto.getAddressTitle());
        address.setEmail(dto.getEmail());
        return address;
    }

    // ============================================================================
    // PAYMENT CREATION (Shared by both DTO versions)
    // ============================================================================

    private Payment createPayment(Order order, PaymentMethod paymentMethod) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(BigDecimal.valueOf(order.getTotalAmount()));
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        return payment; // CASCADE will save
    }

    // ============================================================================
    // EMAIL SENDING (Async, outside transaction)
    // ============================================================================

    @Async
    protected void scheduleOrderConfirmationEmail(Order order) {
        String email = order.getOrderEmail();
        if (email != null) {
            try {
                emailService.sendOrderConfirmationEmail(order, order.getUser());
                log.info("Order confirmation email sent to: {}", email);
            } catch (Exception e) {
                log.error("Failed to send order confirmation email for order {}",
                        order.getId(), e);
            }
        }
    }

    @Async
    protected void scheduleStatusUpdateEmail(Order order, User user, OrderStatus newStatus) {
        String email = order.getOrderEmail();
        if (email != null) {
            try {
                emailService.sendOrderStatusUpdateEmail(order, user, newStatus);
                log.info("Status update email sent to: {}", email);
            } catch (Exception e) {
                log.error("Failed to send status update email to: {}", email, e);
            }
        }
    }

    // ============================================================================
    // PAYMENT PROCESSING
    // ============================================================================
    // NOTE: Card payments are now handled by IyzicoPaymentService
    // See: PaymentController for /api/payment/process and /api/payment/3ds/init
    // endpoints

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public Payment processCashPayment(Long orderId) {
        log.info("Processing cash payment for order ID: {}", orderId);

        Order order = getOrderById(orderId);
        Payment payment = order.getPayment();

        if (payment == null) {
            throw new ResourceNotFoundException("Bu sipariş için ödeme kaydı bulunamadı");
        }

        if (payment.getPaymentMethod() != PaymentMethod.CASH &&
                payment.getPaymentMethod() != PaymentMethod.GIFT_CARD) {
            throw new IllegalArgumentException("Bu ödeme yöntemi nakit olarak işlenemez");
        }

        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setCompletedAt(LocalDateTime.now());
        payment.setTransactionId("CASH_" + System.currentTimeMillis());

        order.setOrderStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        scheduleOrderConfirmationEmail(order);
        orderSearchService.indexOrder(order);

        log.info("Cash payment processed for order ID: {}", orderId);
        return payment;
    }

    // ============================================================================
    // STATUS MANAGEMENT
    // ============================================================================

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public Order updateOrderStatus(Long id, OrderStatus newStatus) {
        log.info("Updating order {} status to: {}", id, newStatus);

        Order order = getOrderById(id);
        User user = order.getUser();
        OrderStatus oldStatus = order.getOrderStatus();

        validateStatusTransition(oldStatus, newStatus);

        order.setOrderStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        scheduleStatusUpdateEmail(order, user, newStatus);

        orderSearchService.indexOrder(updatedOrder);

        log.info("Order {} status updated: {} → {}", id, oldStatus, newStatus);
        return updatedOrder;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void cancelOrder(Long id) {
        log.info("Cancelling order with ID: {}", id);

        Order order = getOrderById(id);
        User user = order.getUser();

        if (order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Teslim edilmiş siparişler iptal edilemez");
        }

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Sipariş zaten iptal edilmiş");
        }

        if (order.getPayment() != null &&
                order.getPayment().getPaymentStatus() == PaymentStatus.SUCCESS) {
            log.info("Order {} has successful payment, marking for refund", id);
            order.getPayment().setPaymentStatus(PaymentStatus.REFUNDED);
        }

        OrderStatus oldStatus = order.getOrderStatus();
        order.setOrderStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        scheduleStatusUpdateEmail(order, user, OrderStatus.CANCELLED);

        orderSearchService.indexOrder(order);

        log.info("Order {} cancelled successfully (previous status: {})", id, oldStatus);
    }

    private void validateStatusTransition(OrderStatus oldStatus, OrderStatus newStatus) {
        if (oldStatus == OrderStatus.DELIVERED && newStatus != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Teslim edilmiş sipariş durumu değiştirilemez");
        }

        if (oldStatus == OrderStatus.CANCELLED && newStatus != OrderStatus.CANCELLED) {
            throw new IllegalStateException("İptal edilmiş sipariş durumu değiştirilemez");
        }

        log.debug("Status transition validated: {} → {}", oldStatus, newStatus);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void deleteOrder(Long id) {
        log.info("Deleting order ID: {}", id);
        Order order = getOrderById(id);
        orderRepository.delete(order);
        orderSearchService.deleteOrderFromIndex(id);
        log.info("Order deleted: ID {}", id);
    }

    // ============================================================================
    // PAGINATION METHODS (Interface Implementation)
    // ============================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<Order> getAllOrders(Pageable pageable) {
        log.debug("Fetching paginated orders - page: {}, size: {}, sort: {}",
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort());

        return orderRepository.findAllWithDetails(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Order> getOrdersByUser(Long userId, Pageable pageable) {
        log.debug("Fetching paginated orders for user ID: {} - page: {}, size: {}",
                userId,
                pageable.getPageNumber(),
                pageable.getPageSize());

        User user = userService.getUserById(userId);
        return orderRepository.findByUserWithDetails(user, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Order> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        log.debug("Fetching paginated orders by status: {} - page: {}, size: {}",
                status,
                pageable.getPageNumber(),
                pageable.getPageSize());

        return orderRepository.findByOrderStatusWithDetails(status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Order> getOrdersBetweenDates(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        log.debug("Fetching orders between {} and {} - page: {}, size: {}",
                startDate,
                endDate,
                pageable.getPageNumber(),
                pageable.getPageSize());

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Başlangıç ve bitiş tarihleri gereklidir");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Başlangıç tarihi bitiş tarihinden sonra olamaz");
        }

        return orderRepository.findOrdersBetweenDates(startDate, endDate, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Order> getOrdersByPriceRange(Double minPrice, Double maxPrice, Pageable pageable) {
        log.debug("Fetching orders by price range: {} - {} - page: {}, size: {}",
                minPrice,
                maxPrice,
                pageable.getPageNumber(),
                pageable.getPageSize());

        if (minPrice == null || maxPrice == null) {
            throw new IllegalArgumentException("Minimum ve maksimum fiyat gereklidir");
        }

        if (minPrice < 0 || maxPrice < 0) {
            throw new IllegalArgumentException("Fiyatlar negatif olamaz");
        }

        if (minPrice > maxPrice) {
            throw new IllegalArgumentException("Minimum fiyat maksimum fiyattan büyük olamaz");
        }

        return orderRepository.findByPriceRange(minPrice, maxPrice, pageable);
    }
}
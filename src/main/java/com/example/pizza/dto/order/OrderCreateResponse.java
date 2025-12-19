package com.example.pizza.dto.order;

import com.example.pizza.constants.order.OrderStatus;
import com.example.pizza.constants.order.PaymentMethod;
import com.example.pizza.constants.order.PaymentStatus;
import com.example.pizza.dto.address.DeliveryAddressResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderCreateResponse {
    private Long orderId;
    private OrderStatus orderStatus;
    private LocalDateTime orderDate;
    private Double totalAmount;
    private String notes;

    // Payment info
    private Long paymentId;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;

    // Delivery address
    private DeliveryAddressResponse deliveryAddress;

    // Items
    private List<OrderItemSummary> items;

    // User info (null for guest)
    private Long userId;
    private String userEmail;
    private String userName;

    // Confirmation
    private String message;
    private String confirmationEmail;
}
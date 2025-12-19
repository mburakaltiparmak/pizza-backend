package com.example.pizza.logic.mapper;

import com.example.pizza.dto.order.OrderItemResponse;
import com.example.pizza.dto.order.OrderResponse;
import com.example.pizza.dto.payment.PaymentResponse;
import com.example.pizza.dto.address.UserAddressResponse;
import com.example.pizza.entity.order.Order;
import com.example.pizza.entity.order.OrderItem;
import com.example.pizza.entity.logic.Payment;
import com.example.pizza.entity.user.UserAddress;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderResponse toOrderResponse(Order order) {
        if (order == null) {
            return null;
        }

        OrderResponse.OrderResponseBuilder builder = OrderResponse.builder()
                .id(order.getId())
                .orderDate(order.getOrderDate())
                .orderStatus(order.getOrderStatus())
                .totalAmount(order.getTotalAmount())
                .notes(order.getNotes())
                .deliveryAddress(toAddressResponse(order.getDeliveryAddress()))
                .items(toOrderItemResponseList(order.getItems()))
                .payment(toPaymentResponse(order.getPayment()));

        if (order.getUser() != null) {
            builder.userId(order.getUser().getId())
                    .userName(order.getUser().getName() + " " + order.getUser().getSurname())
                    .userEmail(order.getUser().getEmail());
        }

        return builder.build();
    }

    public OrderItemResponse toOrderItemResponse(OrderItem item) {
        if (item == null) {
            return null;
        }

        double subtotal = item.getQuantity() * item.getPrice();

        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productName(item.getProduct() != null ? item.getProduct().getName() : "Unknown Product")
                .productImage(item.getProduct() != null ? item.getProduct().getImg() : null)
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .subtotal(subtotal)
                .build();
    }

    public List<OrderItemResponse> toOrderItemResponseList(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        return items.stream()
                .map(this::toOrderItemResponse)
                .collect(Collectors.toList());
    }

    public PaymentResponse toPaymentResponse(Payment payment) {
        if (payment == null) {
            return null;
        }

        return PaymentResponse.builder()
                .id(payment.getId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .transactionId(payment.getTransactionId())
                .errorMessage(payment.getErrorMessage())
                .createdAt(payment.getCreatedAt())
                .completedAt(payment.getCompletedAt())
                .build();
    }

    private UserAddressResponse toAddressResponse(UserAddress address) {
        if (address == null) {
            return null;
        }

        return UserAddressResponse.builder()
                .id(address.getId())
                .fullAddress(address.getFullAddress())
                .city(address.getCity())
                .district(address.getDistrict())
                .postalCode(address.getPostalCode())
                .addressTitle(address.getAddressTitle())
                .phoneNumber(address.getPhoneNumber())
                .recipientName(address.getRecipientName())
                .isDefault(address.getIsDefault())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }
}
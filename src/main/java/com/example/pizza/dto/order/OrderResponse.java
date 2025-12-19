package com.example.pizza.dto.order;

import com.example.pizza.constants.order.OrderStatus;
import com.example.pizza.dto.payment.PaymentResponse;
import com.example.pizza.dto.address.UserAddressResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse implements Serializable {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private UserAddressResponse deliveryAddress;
    private LocalDateTime orderDate;
    private OrderStatus orderStatus;
    private double totalAmount;
    private String notes;
    private List<OrderItemResponse> items;
    private PaymentResponse payment;
}
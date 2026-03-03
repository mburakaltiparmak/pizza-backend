package com.example.pizza.dto.order;

import com.example.pizza.constants.order.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderTrackingResponse implements Serializable {
    private UUID uuid;
    private OrderStatus orderStatus;
    private String monitorStatus; // OVEN, PREPARING, ON_WAY, DELIVERED
    private String estimatedDeliveryTime;
    private List<OrderItemResponse> items;
    private double totalAmount;

    // Masked Data
    private String recipientNameMasked;
    private String deliveryAddressMasked;
}

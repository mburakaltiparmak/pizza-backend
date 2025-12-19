package com.example.pizza.dto.order;

import com.example.pizza.constants.order.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderStatusUpdateRequest {
    @NotNull
    private OrderStatus orderStatus;
}
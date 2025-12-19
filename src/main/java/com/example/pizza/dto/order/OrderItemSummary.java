package com.example.pizza.dto.order;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderItemSummary {
    private Long productId;
    private String productName;
    private String productImage;
    private Integer quantity;
    private Double price;
    private Double subtotal;
}
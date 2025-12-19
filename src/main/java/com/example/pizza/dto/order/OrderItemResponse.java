package com.example.pizza.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse implements Serializable {
    private Long id;
    private Long productId;
    private String productName;
    private String productImage;
    private Integer quantity;
    private double price;
    private double subtotal; // quantity * price
}
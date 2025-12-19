package com.example.pizza.dto.product;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StockErrorResponse {
    private String message;
    private String productName;
    private Integer availableStock;
    private Integer requestedQuantity;
    private LocalDateTime timestamp = LocalDateTime.now();
}
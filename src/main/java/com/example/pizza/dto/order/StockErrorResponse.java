package com.example.pizza.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class StockErrorResponse {
    private String message;
    private String productName;
    private Integer availableStock;
    private Integer requestedQuantity;
    private LocalDateTime timestamp = LocalDateTime.now();
}
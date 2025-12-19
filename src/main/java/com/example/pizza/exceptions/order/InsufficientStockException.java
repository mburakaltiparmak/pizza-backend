package com.example.pizza.exceptions.order;

import lombok.Getter;

@Getter
public class InsufficientStockException extends RuntimeException {

    private final String productName;
    private final Integer availableStock;
    private final Integer requestedQuantity;

    public InsufficientStockException(String message) {
        super(message);
        this.productName = null;
        this.availableStock = null;
        this.requestedQuantity = null;
    }

    public InsufficientStockException(String productName, int availableStock, int requestedQuantity) {
        super(String.format("Yetersiz stok: %s (Mevcut: %d, İstenen: %d)",
                productName, availableStock, requestedQuantity));
        this.productName = productName;
        this.availableStock = availableStock;
        this.requestedQuantity = requestedQuantity;
    }

    public InsufficientStockException(String message, String productName, int availableStock, int requestedQuantity) {
        super(message);
        this.productName = productName;
        this.availableStock = availableStock;
        this.requestedQuantity = requestedQuantity;
    }
}
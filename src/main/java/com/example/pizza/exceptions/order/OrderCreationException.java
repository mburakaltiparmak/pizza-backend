package com.example.pizza.exceptions.order;

public class OrderCreationException extends RuntimeException {

    public OrderCreationException(String message) {
        super(message);
    }

    public OrderCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
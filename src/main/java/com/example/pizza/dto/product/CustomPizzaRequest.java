package com.example.pizza.dto.product;

import lombok.Data;

@Data
public class CustomPizzaRequest {
    private String name;
    private double totalPrice;
    private String customDetails;
}
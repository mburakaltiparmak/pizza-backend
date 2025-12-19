package com.example.pizza.dto.product;

public record ProductRequest(
        String name,
        double rating,
        int stock,
        double price,
        String img,
        Long categoryId
) {}

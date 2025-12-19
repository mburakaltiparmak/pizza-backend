package com.example.pizza.dto.product;

import java.io.Serializable;

public record ProductResponse(
        Long id,
        String name,
        double rating,
        int stock,
        double price,
        String img,
        Long categoryId,
        String categoryName,
        String description
) implements Serializable {}
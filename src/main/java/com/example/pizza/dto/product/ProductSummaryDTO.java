package com.example.pizza.dto.product;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummaryDTO implements Serializable {
    private Long id;
    private String name;
    private Double rating;
    private Integer stock;
    private Double price;
    private String img;
    private String description;

    // Frontend için categoryId
    private Long categoryId;
}

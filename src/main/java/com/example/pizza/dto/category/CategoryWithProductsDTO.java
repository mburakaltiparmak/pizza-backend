package com.example.pizza.dto.category;

import com.example.pizza.dto.product.ProductSummaryDTO;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryWithProductsDTO implements Serializable {
    private Long id;
    private String name;
    private String img;
    private List<ProductSummaryDTO> products;

    // Dashboard için hesaplanmış değerler
    private Integer productCount;
    private Long totalStock;
}
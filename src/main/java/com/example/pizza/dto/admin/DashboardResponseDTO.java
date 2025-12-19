package com.example.pizza.dto.admin;

import com.example.pizza.dto.category.CategoryWithProductsDTO;
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
public class DashboardResponseDTO implements Serializable {
    // Toplam istatistikler
    private Integer totalCategories;
    private Integer totalProducts;
    private Long totalStock;
    private Integer totalUsers;

    // Detaylı veriler
    private List<CategoryWithProductsDTO> categories;
    private List<ProductSummaryDTO> recentProducts;
}
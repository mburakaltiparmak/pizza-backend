package com.example.pizza.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class OrderItemRequest {

    @NotNull(message = "Ürün ID gereklidir")
    @Positive(message = "Ürün ID pozitif olmalıdır")
    private Long productId;

    @NotNull(message = "Miktar gereklidir")
    @Min(value = 1, message = "Miktar en az 1 olmalıdır")
    @Max(value = 100, message = "Bir üründen en fazla 100 adet sipariş edilebilir")
    private Integer quantity;
}
package com.example.pizza.dto.product;

import jakarta.validation.constraints.*;

public record ProductRequest(
                @NotBlank(message = "Ürün adı boş olamaz") @Size(min = 2, max = 100, message = "Ürün adı 2-100 karakter arasında olmalıdır") String name,

                @Min(value = 0, message = "Rating 0'dan küçük olamaz") @Max(value = 5, message = "Rating 5'ten büyük olamaz") double rating,

                @Min(value = 0, message = "Stok negatif olamaz") @Max(value = 10000, message = "Stok 10000'den fazla olamaz") int stock,

                @Positive(message = "Fiyat pozitif olmalıdır") @DecimalMin(value = "0.01", message = "Fiyat en az 0.01 olmalıdır") @DecimalMax(value = "10000.00", message = "Fiyat en fazla 10000 olabilir") double price,

                @NotBlank(message = "Görsel URL boş olamaz") @Size(max = 500, message = "Görsel URL 500 karakterden uzun olamaz") String img,

                @NotNull(message = "Kategori ID gereklidir") @Positive(message = "Kategori ID pozitif olmalıdır") Long categoryId) {
}

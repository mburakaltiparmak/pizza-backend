package com.example.pizza.constants.validation;

public enum ValidationConstants {
    // Password
    MIN_PASSWORD_LENGTH(6, "Minimum şifre uzunluğu"),
    RECOMMENDED_PASSWORD_LENGTH(8, "Önerilen şifre uzunluğu"),

    // Name/Surname
    MAX_NAME_LENGTH(50, "Maximum isim uzunluğu"),
    MIN_NAME_LENGTH(2, "Minimum isim uzunluğu"),
    MAX_SURNAME_LENGTH(50, "Maximum soyisim uzunluğu"),
    MIN_SURNAME_LENGTH(2, "Minimum soyisim uzunluğu"),

    // Address
    MIN_ADDRESS_LENGTH(10, "Minimum adres uzunluğu"),
    MAX_ADDRESS_LENGTH(500, "Maximum adres uzunluğu"),
    MIN_NEIGHBORHOOD_LENGTH(2, "Minimum mahalle uzunluğu"),

    // Phone
    TR_PHONE_LENGTH(10, "Türkiye telefon numarası uzunluğu (başında 0 olmadan)"),

    // Product
    MIN_PRODUCT_PRICE(0.01, "Minimum ürün fiyatı"),
    MAX_PRODUCT_PRICE(10000.0, "Maximum ürün fiyatı"),
    MIN_STOCK(0, "Minimum stok"),
    MAX_STOCK(999999, "Maximum stok"),

    // Order
    MIN_ORDER_ITEMS(1, "Minimum sipariş ürün sayısı"),
    MAX_ORDER_ITEMS(50, "Maximum sipariş ürün sayısı");

    private final Object value;
    private final String description;

    ValidationConstants(Object value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getIntValue() {
        return ((Number) value).intValue();
    }

    public double getDoubleValue() {
        return ((Number) value).doubleValue();
    }

    public String getDescription() {
        return description;
    }
}
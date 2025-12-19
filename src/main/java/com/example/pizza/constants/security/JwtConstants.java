package com.example.pizza.constants.security;

public enum JwtConstants {
    ACCESS_TOKEN_VALIDITY(604800000L, "7 gün"),        // 7 gün (milisaniye) - environment ile sync
    REFRESH_TOKEN_VALIDITY(30 * 24 * 60 * 60 * 1000L, "30 gün"), // 30 gün (milisaniye)
    TOKEN_PREFIX("Bearer ", "JWT token prefix");

    private final Object value;
    private final String description;

    JwtConstants(Object value, String description) {
        this.value = value;
        this.description = description;
    }

    public long getLongValue() {
        return (long) value;
    }

    public String getStringValue() {
        return (String) value;
    }

    public String getDescription() {
        return description;
    }
}
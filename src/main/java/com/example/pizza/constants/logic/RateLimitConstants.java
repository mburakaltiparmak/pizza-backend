package com.example.pizza.constants.logic;

public enum RateLimitConstants {
    LOGIN_REQUESTS_PER_MINUTE(10, "Login endpoint için dakikada maksimum istek"),
    REGISTER_REQUESTS_PER_MINUTE(5, "Register endpoint için dakikada maksimum istek"),
    ORDER_REQUESTS_PER_MINUTE(20, "Sipariş endpoint için dakikada maksimum istek"),
    API_REQUESTS_PER_MINUTE(100, "Genel API istekleri için dakikada maksimum istek");

    private final int limit;
    private final String description;

    RateLimitConstants(int limit, String description) {
        this.limit = limit;
        this.description = description;
    }

    public int getLimit() {
        return limit;
    }

    public String getDescription() {
        return description;
    }
}
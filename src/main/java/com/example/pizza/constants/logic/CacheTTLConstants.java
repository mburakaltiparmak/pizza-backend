package com.example.pizza.constants.logic;

public enum CacheTTLConstants {
    CATEGORIES(600, "10 dakika"),
    PRODUCTS(300, "5 dakika"),
    USERS(180, "3 dakika"),
    DASHBOARD_STATS(60, "1 dakika"),
    PRODUCT_DETAIL(1800, "30 dakika");

    private final int seconds;
    private final String description;

    CacheTTLConstants(int seconds, String description) {
        this.seconds = seconds;
        this.description = description;
    }

    public int getSeconds() {
        return seconds;
    }

    public long getMilliseconds() {
        return seconds * 1000L;
    }

    public String getDescription() {
        return description;
    }
}
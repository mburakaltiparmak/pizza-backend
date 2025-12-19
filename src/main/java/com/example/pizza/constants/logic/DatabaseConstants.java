package com.example.pizza.constants.logic;

public enum DatabaseConstants {
    CUSTOM_CATEGORY_NAME("CUSTOM_BASE", "Custom pizza kategorisi için özel isim"),
    DEFAULT_PAGE_SIZE(20, "Varsayılan sayfalama boyutu"),
    MAX_PAGE_SIZE(100, "Maximum sayfalama boyutu");

    private final Object value;
    private final String description;

    DatabaseConstants(Object value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getStringValue() {
        return (String) value;
    }

    public int getIntValue() {
        return (int) value;
    }

    public String getDescription() {
        return description;
    }
}
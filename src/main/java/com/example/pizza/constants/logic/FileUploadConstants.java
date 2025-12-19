package com.example.pizza.constants.logic;

public enum FileUploadConstants {
    MAX_SIZE(5 * 1024 * 1024, "5 MB"),
    MAX_SIZE_IN_MB(5, "5 MB"),
    IMAGE_JPEG("image/jpeg", "JPEG Format"),
    IMAGE_PNG("image/png", "PNG Format"),
    IMAGE_WEBP("image/webp", "WebP Format");

    private final Object value;
    private final String description;

    FileUploadConstants(Object value, String description) {
        this.value = value;
        this.description = description;
    }

    public Object getValue() {
        return value;
    }

    public int getIntValue() {
        return (int) value;
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
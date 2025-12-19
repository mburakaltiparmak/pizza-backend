package com.example.pizza.constants.logic;

public enum AsyncConfigConstants {
    CORE_POOL_SIZE(5, "Minimum thread sayısı"),
    MAX_POOL_SIZE(10, "Maximum thread sayısı"),
    QUEUE_CAPACITY(100, "Task queue kapasitesi"),
    THREAD_NAME_PREFIX("async-", "Thread isim prefix"),

    // Email executor için (Phase 3)
    EMAIL_CORE_POOL_SIZE(2, "Email thread pool - minimum thread sayısı"),
    EMAIL_MAX_POOL_SIZE(5, "Email thread pool - maximum thread sayısı"),
    EMAIL_QUEUE_CAPACITY(100, "Email task queue kapasitesi"),
    EMAIL_THREAD_PREFIX("email-", "Email thread isim prefix"),

    // File upload executor için (Phase 3)
    FILE_CORE_POOL_SIZE(3, "File upload thread pool - minimum thread sayısı"),
    FILE_MAX_POOL_SIZE(10, "File upload thread pool - maximum thread sayısı"),
    FILE_QUEUE_CAPACITY(50, "File upload task queue kapasitesi"),
    FILE_THREAD_PREFIX("file-", "File upload thread isim prefix"),

    // Shared
    KEEP_ALIVE_SECONDS(60, "Thread idle kalma süresi (saniye)");

    private final Object value;
    private final String description;

    AsyncConfigConstants(Object value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() {
        if (value instanceof Integer) {
            return (int) value;
        }
        throw new IllegalStateException("Value is not an integer");
    }

    public String getStringValue() {
        return (String) value;
    }

    public String getDescription() {
        return description;
    }
}
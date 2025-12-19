package com.example.pizza.constants.validation;


public enum RegexPatternConstants {
    EMAIL("^[A-Za-z0-9+_.-]+@(.+)$", "Email formatı"),
    TURKISH_PHONE("^5[0-9]{9}$", "Türk telefon numarası (5XXXXXXXXX)"),
    POSTAL_CODE("^\\d{5}$", "Posta kodu (5 haneli)"),
    ALPHANUMERIC("^[a-zA-Z0-9]*$", "Alfanümerik karakter");

    private final String pattern;
    private final String description;

    RegexPatternConstants(String pattern, String description) {
        this.pattern = pattern;
        this.description = description;
    }

    public String getPattern() {
        return pattern;
    }

    public String getDescription() {
        return description;
    }
}
package com.example.pizza.util;

import lombok.experimental.UtilityClass;

/**
 * Utility class for masking sensitive data in logs
 * Prevents exposure of PII (Personally Identifiable Information) in log files
 */
@UtilityClass
public class LogMaskingUtil {

    private static final String MASK_CHAR = "*";
    private static final int EMAIL_VISIBLE_CHARS = 2;
    private static final int TOKEN_VISIBLE_CHARS = 4;
    private static final int CARD_VISIBLE_CHARS = 4;

    /**
     * Mask email address
     * Example: john.doe@example.com → jo***@example.com
     *
     * @param email Email address to mask
     * @return Masked email or original if invalid
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return email;
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return email; // Invalid email, return as-is
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (localPart.length() <= EMAIL_VISIBLE_CHARS) {
            return MASK_CHAR.repeat(localPart.length()) + domain;
        }

        String visiblePart = localPart.substring(0, EMAIL_VISIBLE_CHARS);
        String maskedPart = MASK_CHAR.repeat(localPart.length() - EMAIL_VISIBLE_CHARS);

        return visiblePart + maskedPart + domain;
    }

    /**
     * Mask token (JWT, refresh token, etc.)
     * Example: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9... → eyJh***
     *
     * @param token Token to mask
     * @return Masked token showing only first 4 characters
     */
    public static String maskToken(String token) {
        if (token == null || token.isEmpty()) {
            return token;
        }

        if (token.length() <= TOKEN_VISIBLE_CHARS) {
            return MASK_CHAR.repeat(token.length());
        }

        String visiblePart = token.substring(0, TOKEN_VISIBLE_CHARS);
        return visiblePart + MASK_CHAR.repeat(3);
    }

    /**
     * Mask credit card number
     * Example: 4111111111111111 → ************1111
     *
     * @param cardNumber Card number to mask
     * @return Masked card number showing only last 4 digits
     */
    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) {
            return cardNumber;
        }

        // Remove spaces and dashes
        String cleanCard = cardNumber.replaceAll("[\\s-]", "");

        if (cleanCard.length() <= CARD_VISIBLE_CHARS) {
            return MASK_CHAR.repeat(cleanCard.length());
        }

        String maskedPart = MASK_CHAR.repeat(cleanCard.length() - CARD_VISIBLE_CHARS);
        String visiblePart = cleanCard.substring(cleanCard.length() - CARD_VISIBLE_CHARS);

        return maskedPart + visiblePart;
    }

    /**
     * Mask phone number
     * Example: +905551234567 → +9055*****567
     *
     * @param phoneNumber Phone number to mask
     * @return Masked phone number
     */
    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return phoneNumber;
        }

        // Remove spaces and special characters except +
        String cleanPhone = phoneNumber.replaceAll("[\\s()-]", "");

        if (cleanPhone.length() <= 6) {
            return MASK_CHAR.repeat(cleanPhone.length());
        }

        // Show first 4 and last 3 digits
        String prefix = cleanPhone.substring(0, 4);
        String suffix = cleanPhone.substring(cleanPhone.length() - 3);
        String maskedPart = MASK_CHAR.repeat(cleanPhone.length() - 7);

        return prefix + maskedPart + suffix;
    }

    /**
     * Mask generic sensitive data
     * Shows only first 2 and last 2 characters
     *
     * @param data Data to mask
     * @return Masked data
     */
    public static String maskGeneric(String data) {
        if (data == null || data.isEmpty()) {
            return data;
        }

        if (data.length() <= 4) {
            return MASK_CHAR.repeat(data.length());
        }

        String prefix = data.substring(0, 2);
        String suffix = data.substring(data.length() - 2);
        String maskedPart = MASK_CHAR.repeat(data.length() - 4);

        return prefix + maskedPart + suffix;
    }
}

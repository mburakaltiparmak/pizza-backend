package com.example.pizza.constants.security;

/**
 * Token Type Enum
 *
 * Defines the types of JWT tokens used in the system.
 *
 * JWT Refresh Token Flow
 *
 * Token Types:
 * - ACCESS: Short-lived token for API authentication (15-30 minutes)
 * - REFRESH: Long-lived token for obtaining new access tokens (7-30 days)
 *
 * Security Strategy:
 * - Access tokens have short expiration to minimize security risk
 * - Refresh tokens are stored in database and can be revoked
 * - Token rotation: Each refresh request generates a new refresh token
 */
public enum TokenType {

    /**
     * Access Token - Used for API authentication
     * - Short-lived (15-30 minutes)
     * - Sent with every API request
     * - Cannot be revoked (expires naturally)
     * - Stored client-side only
     */
    ACCESS("access_token"),

    /**
     * Refresh Token - Used to obtain new access tokens
     * - Long-lived (7-30 days)
     * - Stored in database with user relationship
     * - Can be revoked (logout, security breach)
     * - One-time use with rotation strategy
     */
    REFRESH("refresh_token");

    private final String value;

    TokenType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
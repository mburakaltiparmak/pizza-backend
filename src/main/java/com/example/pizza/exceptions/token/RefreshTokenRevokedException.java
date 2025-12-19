package com.example.pizza.exceptions.token;

public class RefreshTokenRevokedException extends RuntimeException {

    public RefreshTokenRevokedException(String message) {
        super(message);
    }

    public RefreshTokenRevokedException(String token, String message) {
        super(String.format("Refresh token has been revoked [%s]: %s", token, message));
    }

    public RefreshTokenRevokedException(String token, String message, boolean securityBreach) {
        super(String.format("SECURITY ALERT - Refresh token revoked [%s]: %s (Breach detected: %s)",
                token, message, securityBreach));
    }
}
package com.example.pizza.exceptions.token;

public class RefreshTokenExpiredException extends RuntimeException {

    public RefreshTokenExpiredException(String message) {
        super(message);
    }

    public RefreshTokenExpiredException(String token, String message) {
        super(String.format("Refresh token expired [%s]: %s", token, message));
    }
}
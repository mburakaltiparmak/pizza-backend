package com.example.pizza.exceptions.token;

public class RefreshTokenNotFoundException extends RuntimeException {

    public RefreshTokenNotFoundException(String message) {
        super(message);
    }

    public RefreshTokenNotFoundException(String token, String message) {
        super(String.format("Refresh token not found [%s]: %s", token, message));
    }
}
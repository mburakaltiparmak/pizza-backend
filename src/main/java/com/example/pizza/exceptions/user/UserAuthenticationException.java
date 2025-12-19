package com.example.pizza.exceptions.user;

import com.example.pizza.exceptions.base.ApiException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UserAuthenticationException extends ApiException {
    public UserAuthenticationException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
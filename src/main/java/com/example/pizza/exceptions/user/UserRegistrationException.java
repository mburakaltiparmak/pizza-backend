package com.example.pizza.exceptions.user;

import com.example.pizza.exceptions.base.ApiException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UserRegistrationException extends ApiException {
    public UserRegistrationException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
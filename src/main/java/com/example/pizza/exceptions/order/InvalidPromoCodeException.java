package com.example.pizza.exceptions.order;

import com.example.pizza.exceptions.base.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidPromoCodeException extends ApiException {
    public InvalidPromoCodeException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}

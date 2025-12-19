package com.example.pizza.exceptions.common;

import com.example.pizza.exceptions.base.ApiException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class FileOperationException extends ApiException {
    public FileOperationException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
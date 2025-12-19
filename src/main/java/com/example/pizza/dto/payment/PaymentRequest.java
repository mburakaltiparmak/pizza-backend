package com.example.pizza.dto.payment;

import lombok.Data;

@Data
public class PaymentRequest {
    private String cardNumber;
    private String expireMonth;
    private String expireYear;
    private String cvc;
    private String cardHolderName;
}
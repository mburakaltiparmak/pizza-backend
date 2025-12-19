package com.example.pizza.exceptions.order;

import lombok.Getter;

@Getter
public class PaymentProcessingException extends RuntimeException {

    private final String errorCode;
    private final Long orderId;
    private final Long paymentId;

    public PaymentProcessingException(String message) {
        super(message);
        this.errorCode = "PAYMENT_ERROR";
        this.orderId = null;
        this.paymentId = null;
    }

    public PaymentProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "PAYMENT_ERROR";
        this.orderId = null;
        this.paymentId = null;
    }

    public PaymentProcessingException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.orderId = null;
        this.paymentId = null;
    }

    public PaymentProcessingException(String message, Long orderId, Long paymentId) {
        super(message);
        this.errorCode = "PAYMENT_ERROR";
        this.orderId = orderId;
        this.paymentId = paymentId;
    }

    public PaymentProcessingException(String message, String errorCode, Long orderId, Long paymentId) {
        super(message);
        this.errorCode = errorCode;
        this.orderId = orderId;
        this.paymentId = paymentId;
    }
}

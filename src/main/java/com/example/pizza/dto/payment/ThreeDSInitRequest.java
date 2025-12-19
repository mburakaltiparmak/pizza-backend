package com.example.pizza.dto.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ThreeDSInitRequest {

    @NotNull(message = "Sipariş ID'si zorunludur")
    private Long orderId;

    @Valid
    @NotNull(message = "Kart bilgileri zorunludur")
    private PaymentCardRequest card;

    // Optional callback URL override (default from config used if null)
    private String callbackUrl;
}

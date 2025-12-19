package com.example.pizza.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CardPaymentRequest {
    @NotBlank(message = "Kart numarası gereklidir.")
    @Pattern(regexp = "^[0-9]{13,19}$", message = "Geçerli bir kart numarası giriniz")
    private String cardNumber;
}

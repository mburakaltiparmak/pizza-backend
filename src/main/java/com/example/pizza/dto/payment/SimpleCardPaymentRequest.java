package com.example.pizza.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SimpleCardPaymentRequest {
    @NotBlank(message = "Kart numarası gereklidir")
    @Pattern(regexp = "^[0-9]{16}$", message = "Kart numarası 16 haneli olmalıdır")
    private String cardNumber;

    private String nameOnCard;

    @NotBlank(message = "Son kullanma ayı gereklidir")
    @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Son kullanma ayı 01-12 arasında olmalıdır")
    private String expirationMonth;

    @NotBlank(message = "Son kullanma yılı gereklidir")
    @Pattern(regexp = "^[0-9]{2}$", message = "Son kullanma yılı 2 haneli olmalıdır")
    private String expirationYear;

    @NotBlank(message = "CVC gereklidir")
    @Pattern(regexp = "^[0-9]{3,4}$", message = "CVC 3 veya 4 haneli olmalıdır")
    private String cvc;
}
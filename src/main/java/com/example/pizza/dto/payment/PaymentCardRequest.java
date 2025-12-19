package com.example.pizza.dto.payment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCardRequest {

    @NotBlank(message = "Kart sahibi adı zorunludur")
    private String cardHolderName;

    @NotBlank(message = "Kart numarası zorunludur")
    @Pattern(regexp = "^[0-9]{16}$", message = "Kart numarası 16 haneli olmalıdır")
    private String cardNumber;

    @NotBlank(message = "Son kullanma ayı zorunludur")
    @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Ay 01-12 arası olmalıdır")
    private String expireMonth;

    @NotBlank(message = "Son kullanma yılı zorunludur")
    @Pattern(regexp = "^20[0-9]{2}$", message = "Yıl 20XX formatında olmalıdır")
    private String expireYear;

    @NotBlank(message = "CVC zorunludur")
    @Pattern(regexp = "^[0-9]{3,4}$", message = "CVC 3 veya 4 haneli olmalıdır")
    private String cvc;

    @Builder.Default
    private Integer registerCard = 0; // 0=Don't save, 1=Save

    @Min(value = 1, message = "Taksit sayısı en az 1 olmalıdır")
    @Max(value = 12, message = "Taksit sayısı en fazla 12 olabilir")
    @Builder.Default
    private Integer installment = 1;
}

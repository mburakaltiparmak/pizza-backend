package com.example.pizza.dto.order;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelRequest {

    @NotEmpty(message = "E-posta adresi zorunludur")
    @Email(message = "Geçerli bir e-posta adresi giriniz")
    private String email;
}

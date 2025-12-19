package com.example.pizza.dto.address;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeliveryAddressRequest {

    @NotBlank(message = "Tam adres gereklidir")
    @Size(max = 500, message = "Adres en fazla 500 karakter olabilir")
    private String fullAddress;

    @NotBlank(message = "Şehir gereklidir")
    @Size(max = 100, message = "Şehir adı en fazla 100 karakter olabilir")
    private String city;

    @NotBlank(message = "İlçe gereklidir")
    @Size(max = 100, message = "İlçe adı en fazla 100 karakter olabilir")
    private String district;

    @Size(max = 20, message = "Posta kodu en fazla 20 karakter olabilir")
    private String postalCode;

    @Size(max = 100, message = "Adres başlığı en fazla 100 karakter olabilir")
    private String addressTitle;

    @NotBlank(message = "Telefon numarası gereklidir")
    @Pattern(
            regexp = "^(\\+90|0)?[0-9]{10}$",
            message = "Geçerli telefon numarası giriniz (örn: 05551234567)"
    )
    private String phoneNumber;

    @NotBlank(message = "Alıcı adı gereklidir")
    @Size(min = 2, max = 100, message = "Alıcı adı 2-100 karakter arasında olmalıdır")
    private String recipientName;

    @Email(message = "Geçerli email adresi giriniz")
    @Size(max = 255, message = "Email en fazla 255 karakter olabilir")
    private String email;
}
package com.example.pizza.dto.address;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserAddressDto {

    private Long id;  // Kayıtlı adres için (opsiyonel)

    @NotBlank(message = "Adres gereklidir")
    private String fullAddress;

    @NotBlank(message = "Şehir gereklidir")
    private String city;

    @NotBlank(message = "İlçe gereklidir")
    private String district;

    private String postalCode;

    private String addressTitle;

    // Misafir için zorunlu alanlar (kayıtlı kullanıcı için opsiyonel)
    @Pattern(regexp = "^(\\+90|0)?[0-9]{10}$", message = "Geçerli telefon numarası giriniz")
    private String phoneNumber;

    @NotBlank(message = "Alıcı adı gereklidir")
    private String recipientName;

    @Email(message = "Geçerli email giriniz")
    private String email;  // Misafir için zorunlu

    private Boolean isDefault;
}
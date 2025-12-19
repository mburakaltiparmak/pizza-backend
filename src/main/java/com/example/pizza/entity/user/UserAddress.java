package com.example.pizza.entity.user;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAddress {

    @Column(name = "address_id", nullable = true)
    private Long id;

    @Column(name = "full_address", nullable = false)
    @NotBlank(message = "Adres alanı gereklidir")
    private String fullAddress;

    @Column(name = "city", nullable = false)
    @NotBlank(message = "Şehir alanı gereklidir")
    private String city;

    @Column(name = "district", nullable = false)
    @NotBlank(message = "İlçe alanı gereklidir")
    private String district;

    @Column(name = "postal_code", nullable = true)
    private String postalCode;

    @Column(name = "address_title", nullable = true)
    private String addressTitle;

    @Column(name = "phone_number", nullable = true)
    private String phoneNumber;

    @Column(name = "recipient_name", nullable = true)
    private String recipientName;

    // Misafir siparişleri için email alanı
    @Column(name = "email", nullable = true)
    private String email;

    @Column(name = "is_default", nullable = true)
    private Boolean isDefault = false;

    @Column(name = "created_at", nullable = true)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = true)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
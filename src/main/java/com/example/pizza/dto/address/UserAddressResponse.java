package com.example.pizza.dto.address;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAddressResponse implements Serializable {
    private Long id;
    private String fullAddress;
    private String city;
    private String district;
    private String postalCode;
    private String addressTitle;
    private String phoneNumber;
    private String recipientName;
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
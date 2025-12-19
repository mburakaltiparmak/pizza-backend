package com.example.pizza.dto.address;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeliveryAddressResponse {
    private String fullAddress;
    private String city;
    private String district;
    private String postalCode;
    private String addressTitle;
    private String phoneNumber;
    private String recipientName;
}
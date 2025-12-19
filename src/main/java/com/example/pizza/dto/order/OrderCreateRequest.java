package com.example.pizza.dto.order;

import com.example.pizza.constants.order.PaymentMethod;
import com.example.pizza.dto.address.DeliveryAddressRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class OrderCreateRequest {

    @NotEmpty(message = "Sipariş en az bir ürün içermelidir")
    @Valid
    private List<OrderItemRequest> items;
    private Long addressId;

    @Valid
    private DeliveryAddressRequest newAddress;

    @NotNull(message = "Ödeme yöntemi gereklidir")
    private PaymentMethod paymentMethod;

    @Size(max = 500, message = "Not 500 karakterden uzun olamaz")
    private String notes;
}
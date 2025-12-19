package com.example.pizza.logic.validator;

import com.example.pizza.constants.order.PaymentMethod;
import com.example.pizza.dto.address.DeliveryAddressRequest;
import com.example.pizza.dto.order.OrderCreateRequest;
import com.example.pizza.dto.order.OrderItemRequest;
import com.example.pizza.entity.user.User;
import com.example.pizza.exceptions.base.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class OrderValidator {

    public void validateOrderRequest(OrderCreateRequest request, User user) {
        log.debug("Validating order request for {}", user != null ? user.getEmail() : "guest");

        validateNotNull(request, "Sipariş bilgileri");
        validateItems(request.getItems());
        validateAddress(request, user);
        validatePaymentMethod(request.getPaymentMethod());
        validateNotes(request.getNotes());
    }

    private void validateItems(List<OrderItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new ValidationException("Sipariş en az bir ürün içermelidir");
        }

        // Check for negative or zero quantities
        for (OrderItemRequest item : items) {
            if (item.getProductId() == null) {
                throw new ValidationException("Ürün ID gereklidir");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new ValidationException("Geçersiz miktar: " + item.getQuantity());
            }
        }

        // Check for duplicate products
        Set<Long> productIds = new HashSet<>();
        for (OrderItemRequest item : items) {
            if (!productIds.add(item.getProductId())) {
                throw new ValidationException("Aynı ürün birden fazla kez eklenemez: ID " + item.getProductId());
            }
        }

        log.debug("Items validation passed: {} unique products", productIds.size());
    }

    private void validateAddress(OrderCreateRequest request, User user) {
        boolean hasAddressId = request.getAddressId() != null;
        boolean hasNewAddress = request.getNewAddress() != null;

        // Must have at least one address option
        if (!hasAddressId && !hasNewAddress) {
            throw new ValidationException("Teslimat adresi gereklidir (addressId veya newAddress)");
        }

        // Cannot have both
        if (hasAddressId && hasNewAddress) {
            throw new ValidationException("addressId ve newAddress birlikte kullanılamaz");
        }

        // Saved address requires authentication
        if (hasAddressId && user == null) {
            throw new ValidationException("Kayıtlı adres kullanmak için giriş yapmalısınız");
        }

        // Guest orders must use new address
        if (user == null && !hasNewAddress) {
            throw new ValidationException("Misafir siparişi için yeni adres bilgileri gereklidir");
        }

        // Validate new address for guest orders
        if (user == null && hasNewAddress) {
            validateGuestAddress(request.getNewAddress());
        }

        log.debug("Address validation passed");
    }

    private void validateGuestAddress(DeliveryAddressRequest address) {
        if (address.getEmail() == null || address.getEmail().trim().isEmpty()) {
            throw new ValidationException("Misafir siparişi için email adresi gereklidir");
        }

        if (address.getRecipientName() == null || address.getRecipientName().trim().isEmpty()) {
            throw new ValidationException("Misafir siparişi için alıcı adı gereklidir");
        }

        if (address.getPhoneNumber() == null || address.getPhoneNumber().trim().isEmpty()) {
            throw new ValidationException("Misafir siparişi için telefon numarası gereklidir");
        }

        log.debug("Guest address validation passed");
    }

    private void validatePaymentMethod(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            throw new ValidationException("Ödeme yöntemi gereklidir");
        }

        // Could add additional payment method specific validations here
        log.debug("Payment method validation passed: {}", paymentMethod);
    }

    private void validateNotes(String notes) {
        if (notes != null && notes.length() > 500) {
            throw new ValidationException("Sipariş notu 500 karakterden uzun olamaz");
        }
    }

    private void validateNotNull(Object object, String fieldName) {
        if (object == null) {
            throw new ValidationException(fieldName + " boş olamaz");
        }
    }
}
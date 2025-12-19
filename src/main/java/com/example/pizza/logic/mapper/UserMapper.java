package com.example.pizza.logic.mapper;

import com.example.pizza.dto.address.UserAddressResponse;
import com.example.pizza.dto.user.UserResponse;
import com.example.pizza.entity.user.User;
import com.example.pizza.entity.user.UserAddress;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .surname(user.getSurname())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .status(user.getStatus())
                .oauthProvider(user.getOauthProvider())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLogin())
                .addresses(toAddressResponseListSafe(user.getAddresses()))  // ✅ FIXED: Safe version
                .build();
    }

    public UserAddressResponse toAddressResponse(UserAddress address) {
        if (address == null) {
            return null;
        }

        return UserAddressResponse.builder()
                .id(address.getId())
                .fullAddress(address.getFullAddress())
                .city(address.getCity())
                .district(address.getDistrict())
                .postalCode(address.getPostalCode())
                .addressTitle(address.getAddressTitle())
                .phoneNumber(address.getPhoneNumber())
                .recipientName(address.getRecipientName())
                .isDefault(address.getIsDefault())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }

    public List<UserAddressResponse> toAddressResponseList(List<UserAddress> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return Collections.emptyList();
        }

        return addresses.stream()
                .map(this::toAddressResponse)
                .collect(Collectors.toList());
    }

    public List<UserAddressResponse> toAddressResponseListSafe(List<UserAddress> addresses) {
        // Null check
        if (addresses == null) {
            return Collections.emptyList();
        }

        // CRITICAL FIX: Check if collection is initialized
        // Hibernate.isInitialized() returns false if lazy-loaded and not fetched
        if (!Hibernate.isInitialized(addresses)) {
            // Collection not loaded, return empty list
            // This prevents LazyInitializationException
            return Collections.emptyList();
        }

        // Collection is loaded, safe to access
        if (addresses.isEmpty()) {
            return Collections.emptyList();
        }

        return addresses.stream()
                .map(this::toAddressResponse)
                .collect(Collectors.toList());
    }
}

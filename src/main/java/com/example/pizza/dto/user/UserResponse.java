package com.example.pizza.dto.user;

import com.example.pizza.constants.user.Role;
import com.example.pizza.constants.user.UserStatus;
import com.example.pizza.dto.address.UserAddressResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse implements Serializable {
    private Long id;
    private String name;
    private String surname;
    private String email;
    private String phoneNumber;
    private Role role;
    private UserStatus status;
    private String oauthProvider;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private List<UserAddressResponse> addresses;

}
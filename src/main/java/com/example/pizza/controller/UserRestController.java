package com.example.pizza.controller;

import com.example.pizza.dto.user.PasswordChangeRequest;
import com.example.pizza.dto.address.UserAddressDto;
import com.example.pizza.dto.address.UserAddressResponse;
import com.example.pizza.dto.user.UserResponse;
import com.example.pizza.entity.user.User;
import com.example.pizza.dto.exceptions.ApiError;
import com.example.pizza.logic.mapper.UserMapper;
import com.example.pizza.service.user.UserService;
import com.example.pizza.service.logic.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserRestController {

    private final UserService userService;
    private final EmailService emailService;
    private final UserMapper userMapper;
    private static final Logger logger = LoggerFactory.getLogger(UserRestController.class);

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userService.getUserByEmail(email);
        UserResponse response = userMapper.toUserResponse(user);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateUserProfile(@Valid @RequestBody User updatedUser) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        User updated = userService.updateUserProfile(user.getId(), updatedUser);
        UserResponse response = userMapper.toUserResponse(updated);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User user = userService.getUserByEmail(email);

            userService.changePassword(user.getId(), request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok().body(Collections.singletonMap("message", "Şifre başarıyla değiştirildi"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
        } catch (Exception e) {
            logger.error("Password change error", e);
            return ResponseEntity.badRequest().body(new ApiError("Şifre değiştirme hatası: " + e.getMessage()));
        }
    }
    @GetMapping("/addresses")
    public ResponseEntity<List<UserAddressResponse>> getUserAddresses() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            logger.info("Address request for user: {}", email);

            // Service metodu addresses'leri EAGER olarak yükler (JOIN FETCH)
            User user = userService.getUserByEmailWithAddresses(email);

            // Artık addresses koleksiyonu yüklenmiş durumda, LazyInitializationException olmaz
            List<UserAddressResponse> addresses = userMapper.toAddressResponseList(user.getAddresses());

            return ResponseEntity.ok(addresses);
        } catch (Exception e) {
            logger.error("Error fetching addresses for user", e);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }
    @PostMapping("/addresses")
    public ResponseEntity<?> addAddress(@Valid @RequestBody UserAddressDto addressDto) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User user = userService.getUserByEmail(email);

            // Service metodu transaction içinde çalışır
            User updatedUser = userService.addAddress(user.getId(), addressDto);
            List<UserAddressResponse> addresses = userMapper.toAddressResponseList(updatedUser.getAddresses());

            return ResponseEntity.ok(addresses);
        } catch (IllegalArgumentException e) {
            logger.warn("Address validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error adding address", e);
            return ResponseEntity.badRequest().body(new ApiError("Adres eklenirken hata: " + e.getMessage()));
        }
    }

    @PutMapping("/addresses/{id}")
    public ResponseEntity<?> updateAddress(
            @PathVariable int id,
            @Valid @RequestBody UserAddressDto addressDto) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User user = userService.getUserByEmail(email);

            User updatedUser = userService.updateAddress(user.getId(), id, addressDto);
            List<UserAddressResponse> addresses = userMapper.toAddressResponseList(updatedUser.getAddresses());

            return ResponseEntity.ok(addresses);
        } catch (IllegalArgumentException e) {
            logger.warn("Address validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating address", e);
            return ResponseEntity.badRequest().body(new ApiError("Adres güncellenirken hata: " + e.getMessage()));
        }
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<?> deleteAddress(@PathVariable int id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User user = userService.getUserByEmail(email);

            User updatedUser = userService.removeAddress(user.getId(), id);
            List<UserAddressResponse> addresses = userMapper.toAddressResponseList(updatedUser.getAddresses());

            return ResponseEntity.ok(addresses);
        } catch (IllegalArgumentException e) {
            logger.warn("Address deletion validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiError(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error deleting address", e);
            return ResponseEntity.badRequest().body(new ApiError("Adres silinirken hata: " + e.getMessage()));
        }
    }


    @PutMapping("/addresses/{id}/default")
    public ResponseEntity<?> setDefaultAddress(@PathVariable int id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            User user = userService.getUserByEmail(email);

            User updatedUser = userService.setDefaultAddress(user.getId(), id);
            List<UserAddressResponse> addresses = userMapper.toAddressResponseList(updatedUser.getAddresses());

            return ResponseEntity.ok(addresses);
        } catch (Exception e) {
            logger.error("Error setting default address", e);
            return ResponseEntity.badRequest().body(new ApiError("Varsayılan adres ayarlanırken hata: " + e.getMessage()));
        }
    }
}
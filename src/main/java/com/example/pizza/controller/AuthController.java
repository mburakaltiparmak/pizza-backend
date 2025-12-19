package com.example.pizza.controller;

import com.example.pizza.config.security.UnifiedTokenProvider;
import com.example.pizza.constants.user.Role;
import com.example.pizza.dto.exceptions.ApiResponse;
import com.example.pizza.dto.user.*;
import com.example.pizza.entity.token.RefreshToken;
import com.example.pizza.entity.user.User;
import com.example.pizza.dto.exceptions.ApiError;
import com.example.pizza.exceptions.token.RefreshTokenExpiredException;
import com.example.pizza.exceptions.token.RefreshTokenNotFoundException;
import com.example.pizza.exceptions.token.RefreshTokenRevokedException;
import com.example.pizza.exceptions.user.UserRegistrationException;
import com.example.pizza.service.logic.RefreshTokenService;
import com.example.pizza.service.user.SupabaseUserService;
import com.example.pizza.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import jakarta.validation.Valid;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final UnifiedTokenProvider tokenProvider;
    private final SupabaseUserService supabaseUserService;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.supabase.url}")
    private String supabaseUrl;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            // Register in our database directly
            User localUser = new User();
            localUser.setName(registerRequest.getName());
            localUser.setSurname(registerRequest.getSurname());
            localUser.setEmail(registerRequest.getEmail());
            localUser.setPhoneNumber(registerRequest.getPhoneNumber());
            localUser.setPassword(registerRequest.getPassword()); // Will be hashed by service

            User registeredUser = userService.registerUser(localUser);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    new ApiResponse(true, "Kullanıcı başarıyla kaydedildi. Lütfen email adresinizi doğrulayın.")
            );
        } catch (UserRegistrationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiError(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error during registration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError("Kayıt işlemi sırasında bir hata oluştu."));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {
        try {
            // Authenticate user credentials
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Get user details
            User user = userService.getUserByEmail(loginRequest.getEmail());

            // Generate access token (short-lived)
            String accessToken = tokenProvider.generateAccessToken(authentication);

            // Create refresh token (long-lived, database-stored)
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, request);

            // Update last login time
            userService.updateLoginTime(loginRequest.getEmail());

            logger.info("User authenticated successfully: {} (Access token: 30min, Refresh token: 7 days)",
                    loginRequest.getEmail());

            // Return both tokens
            return ResponseEntity.ok(AuthenticationResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken.getToken())
                    .tokenType("Bearer")
                    .expiresIn(tokenProvider.getAccessTokenExpiration() / 1000) // Convert to seconds
                    .build());

        } catch (BadCredentialsException e) {
            logger.warn("Login failed - invalid credentials for: {}", loginRequest.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("Geçersiz email veya şifre."));
        } catch (Exception e) {
            logger.error("Login error for user: {}", loginRequest.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError("Giriş işlemi sırasında bir hata oluştu."));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @Valid @RequestBody RefreshTokenRequest refreshRequest,
            HttpServletRequest request) {
        try {
            String requestRefreshToken = refreshRequest.getRefreshToken();

            logger.debug("Refresh token request received");

            // Validate and rotate refresh token (one-time use pattern)
            RefreshToken newRefreshToken = refreshTokenService.rotateToken(requestRefreshToken, request);

            // Get user from refresh token
            User user = newRefreshToken.getUser();

            // Generate new access token
            String newAccessToken = tokenProvider.generateAccessToken(
                    user.getEmail(),
                    user.getAuthorities()
            );

            logger.info("Token refreshed successfully for user: {}", user.getEmail());

            // Return new tokens
            return ResponseEntity.ok(AuthenticationResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken.getToken())
                    .tokenType("Bearer")
                    .expiresIn(tokenProvider.getAccessTokenExpiration() / 1000)
                    .build());

        } catch (RefreshTokenNotFoundException e) {
            logger.warn("Refresh token not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiError("Geçersiz refresh token. Lütfen tekrar giriş yapın."));
        } catch (RefreshTokenExpiredException e) {
            logger.warn("Refresh token expired: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("Refresh token süresi dolmuş. Lütfen tekrar giriş yapın."));
        } catch (RefreshTokenRevokedException e) {
            logger.error("SECURITY ALERT - Revoked token used: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiError("Token iptal edilmiş. Güvenlik nedeniyle tüm oturumlarınız sonlandırıldı."));
        } catch (Exception e) {
            logger.error("Error refreshing token", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError("Token yenileme sırasında bir hata oluştu."));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody(required = false) RefreshTokenRequest refreshRequest) {
        try {
            // Get current user from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated()) {
                String email = authentication.getName();
                User user = userService.getUserByEmail(email);

                // If refresh token provided, revoke it specifically
                if (refreshRequest != null && refreshRequest.getRefreshToken() != null) {
                    refreshTokenService.revokeToken(refreshRequest.getRefreshToken());
                    logger.info("Specific refresh token revoked for user: {}", email);
                } else {
                    // Otherwise revoke all refresh tokens for user
                    int revokedCount = refreshTokenService.revokeAllUserTokens(user.getId());
                    logger.info("All refresh tokens revoked for user: {} (count: {})", email, revokedCount);
                }

                // Clear security context
                SecurityContextHolder.clearContext();

                return ResponseEntity.ok(new ApiResponse(true, "Başarıyla çıkış yapıldı."));
            }

            return ResponseEntity.ok(new ApiResponse(true, "Çıkış yapıldı."));

        } catch (Exception e) {
            logger.error("Error during logout", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError("Çıkış işlemi sırasında bir hata oluştu."));
        }
    }

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        try {
            boolean verified = userService.verifyEmail(token);
            if (verified) {
                return ResponseEntity.ok(new ApiResponse(true, "Email adresiniz başarıyla doğrulandı."));
            } else {
                return ResponseEntity.badRequest().body(new ApiError("Geçersiz veya süresi dolmuş doğrulama token'ı."));
            }
        } catch (Exception e) {
            logger.error("Error verifying email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError("Email doğrulama işlemi sırasında bir hata oluştu."));
        }
    }

    @PostMapping("/supabase/callback")
    public ResponseEntity<?> handleSupabaseCallback(@RequestBody SupabaseUserDto supabaseUser) {
        try {
            // Validate required fields
            if (supabaseUser.getEmail() == null || supabaseUser.getEmail().isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiError("Email gereklidir"));
            }

            // Sync user with Supabase (creates or updates)
            User user = supabaseUserService.syncSupabaseUser(supabaseUser);

            // Generate application token (using deprecated method for Supabase compatibility)
            String appToken = tokenProvider.generateToken(user.getEmail(), user.getAuthorities());

            logger.info("Supabase callback successful for user: {}", user.getEmail());

            return ResponseEntity.ok(new AuthenticationResponse(appToken));
        } catch (Exception e) {
            logger.error("Supabase callback error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError("Supabase kimlik doğrulama hatası"));
        }
    }

    @GetMapping("/pending-users")
    public ResponseEntity<?> getPendingUsers() {
        return ResponseEntity.ok(userService.getPendingUsers());
    }

    @PostMapping("/approve/{userId}")
    public ResponseEntity<?> approveUser(@PathVariable Long userId) {
        userService.approveUser(userId);
        return ResponseEntity.ok(new ApiResponse(true, "Kullanıcı onaylandı."));
    }

    @PostMapping("/reject/{userId}")
    public ResponseEntity<?> rejectUser(@PathVariable Long userId) {
        userService.rejectUser(userId);
        return ResponseEntity.ok(new ApiResponse(true, "Kullanıcı reddedildi."));
    }

    @PostMapping("/change-role/{userId}")
    public ResponseEntity<?> changeUserRole(@PathVariable Long userId, @RequestParam Role role) {
        userService.updateUserRole(userId, role);
        return ResponseEntity.ok(new ApiResponse(true, "Kullanıcı rolü güncellendi."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiError("Email adresi gereklidir."));
        }

        try {
            boolean sent = userService.sendPasswordResetEmail(email);
            if (sent) {
                return ResponseEntity.ok(new ApiResponse(true, "Şifre sıfırlama bağlantısı email adresinize gönderildi."));
            } else {
                return ResponseEntity.badRequest().body(new ApiError("Bu email adresine sahip bir kullanıcı bulunamadı."));
            }
        } catch (Exception e) {
            logger.error("Error sending password reset email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError("Şifre sıfırlama işlemi sırasında bir hata oluştu."));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");

        if (token == null || token.isEmpty() || newPassword == null || newPassword.isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiError("Token ve yeni şifre gereklidir."));
        }

        try {
            boolean reset = userService.resetPassword(token, newPassword);
            if (reset) {
                return ResponseEntity.ok(new ApiResponse(true, "Şifreniz başarıyla sıfırlandı."));
            } else {
                return ResponseEntity.badRequest().body(new ApiError("Geçersiz veya süresi dolmuş token."));
            }
        } catch (Exception e) {
            logger.error("Error resetting password", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError("Şifre sıfırlama işlemi sırasında bir hata oluştu."));
        }
    }
}
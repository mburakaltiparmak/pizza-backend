package com.example.pizza.service.logic;

import com.example.pizza.entity.token.RefreshToken;
import com.example.pizza.entity.user.User;
import com.example.pizza.exceptions.token.RefreshTokenExpiredException;
import com.example.pizza.exceptions.token.RefreshTokenNotFoundException;
import com.example.pizza.exceptions.token.RefreshTokenRevokedException;
import com.example.pizza.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * RefreshToken Service Implementation
 *
 * Business logic implementation for refresh token management.
 * Handles token lifecycle, validation, rotation, and security features.
 *
 *
 * Key Features:
 * - Token creation with device tracking
 * - Token validation with comprehensive checks
 * - Token rotation (one-time use pattern)
 * - Reuse detection for security
 * - Cleanup operations for maintenance
 *
 * Security Measures:
 * - All user tokens revoked on reuse detection
 * - Device tracking (user agent, IP) for audit
 * - Automatic expiration validation
 * - Transaction management for consistency
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Refresh token expiration time in milliseconds
     * Default: 604800000ms = 7 days
     * Can be configured via application.properties: jwt.refresh-expiration
     */
    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshTokenExpirationMs;

    // ============================================================================
    // TOKEN CREATION
    // ============================================================================

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        log.debug("Creating refresh token for user: {}", user.getEmail());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenExpirationMs))
                .revoked(false)
                .build();

        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);
        log.info("Refresh token created successfully for user: {} (expires: {})",
                user.getEmail(), savedToken.getExpiryDate());

        return savedToken;
    }

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user, HttpServletRequest request) {
        log.debug("Creating refresh token with device tracking for user: {}", user.getEmail());

        // Extract device information from request
        String userAgent = extractUserAgent(request);
        String ipAddress = extractIpAddress(request);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenExpirationMs))
                .revoked(false)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .build();

        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);
        log.info("Refresh token created with device tracking for user: {} (IP: {}, expires: {})",
                user.getEmail(), ipAddress, savedToken.getExpiryDate());

        return savedToken;
    }

    // ============================================================================
    // TOKEN VALIDATION
    // ============================================================================

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByToken(String token) {
        log.debug("Finding refresh token: {}", maskToken(token));
        return refreshTokenRepository.findByToken(token);
    }

    @Override
    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        log.debug("Verifying expiration for token: {}", maskToken(token.getToken()));

        if (token.isExpired()) {
            log.warn("Refresh token expired: {} (expired at: {})",
                    maskToken(token.getToken()), token.getExpiryDate());
            throw new RefreshTokenExpiredException(
                    maskToken(token.getToken()),
                    "Refresh token expired. Please login again."
            );
        }

        // Update last used timestamp
        token.updateLastUsed();
        refreshTokenRepository.save(token);

        log.debug("Token verified successfully: {}", maskToken(token.getToken()));
        return token;
    }

    @Override
    @Transactional
    public RefreshToken validateToken(String tokenString) {
        log.debug("Validating refresh token: {}", maskToken(tokenString));

        // 1. Check if token exists
        RefreshToken token = findByToken(tokenString)
                .orElseThrow(() -> {
                    log.warn("Refresh token not found: {}", maskToken(tokenString));
                    return new RefreshTokenNotFoundException(
                            maskToken(tokenString),
                            "Token not found. It may have been used or deleted."
                    );
                });

        // 2. Check if token is revoked
        if (token.isRevoked()) {
            log.error("SECURITY ALERT: Attempt to use revoked token: {} (User: {})",
                    maskToken(tokenString), token.getUser().getEmail());
            throw new RefreshTokenRevokedException(
                    maskToken(tokenString),
                    "Token has been revoked. Please login again.",
                    true // Security breach flag
            );
        }

        // 3. Verify expiration and update last used
        return verifyExpiration(token);
    }

    // ============================================================================
    // TOKEN ROTATION
    // ============================================================================

    @Override
    @Transactional
    public RefreshToken rotateToken(String oldToken) {
        log.debug("Rotating refresh token: {}", maskToken(oldToken));

        // 1. Validate old token
        RefreshToken oldRefreshToken = validateToken(oldToken);
        User user = oldRefreshToken.getUser();

        // 2. Delete old token (one-time use)
        refreshTokenRepository.delete(oldRefreshToken);
        log.debug("Old token deleted: {}", maskToken(oldToken));

        // 3. Create new token
        RefreshToken newToken = createRefreshToken(user);

        log.info("Token rotation successful for user: {} (old: {}, new: {})",
                user.getEmail(), maskToken(oldToken), maskToken(newToken.getToken()));

        return newToken;
    }

    @Override
    @Transactional
    public RefreshToken rotateToken(String oldToken, HttpServletRequest request) {
        log.debug("Rotating refresh token with device tracking: {}", maskToken(oldToken));

        // 1. Validate old token
        RefreshToken oldRefreshToken = validateToken(oldToken);
        User user = oldRefreshToken.getUser();

        // 2. Delete old token (one-time use)
        refreshTokenRepository.delete(oldRefreshToken);
        log.debug("Old token deleted: {}", maskToken(oldToken));

        // 3. Create new token with device tracking
        RefreshToken newToken = createRefreshToken(user, request);

        log.info("Token rotation with device tracking successful for user: {} (old: {}, new: {})",
                user.getEmail(), maskToken(oldToken), maskToken(newToken.getToken()));

        return newToken;
    }

    // ============================================================================
    // TOKEN REVOCATION
    // ============================================================================

    @Override
    @Transactional
    public boolean revokeToken(String tokenString) {
        log.debug("Revoking refresh token: {}", maskToken(tokenString));

        int revokedCount = refreshTokenRepository.revokeToken(tokenString);

        if (revokedCount > 0) {
            log.info("Refresh token revoked: {}", maskToken(tokenString));
            return true;
        }

        log.warn("Token not found for revocation: {}", maskToken(tokenString));
        return false;
    }

    @Override
    @Transactional
    public boolean deleteToken(String tokenString) {
        log.debug("Deleting refresh token: {}", maskToken(tokenString));

        int deletedCount = refreshTokenRepository.deleteByToken(tokenString);

        if (deletedCount > 0) {
            log.info("Refresh token deleted: {}", maskToken(tokenString));
            return true;
        }

        log.warn("Token not found for deletion: {}", maskToken(tokenString));
        return false;
    }

    @Override
    @Transactional
    public int revokeAllUserTokens(Long userId) {
        log.debug("Revoking all refresh tokens for user ID: {}", userId);

        int revokedCount = refreshTokenRepository.revokeAllUserTokens(userId);

        log.info("Revoked {} refresh tokens for user ID: {}", revokedCount, userId);
        return revokedCount;
    }

    @Override
    @Transactional
    public int deleteAllUserTokens(Long userId) {
        log.debug("Deleting all refresh tokens for user ID: {}", userId);

        int deletedCount = refreshTokenRepository.deleteByUserId(userId);

        log.info("Deleted {} refresh tokens for user ID: {}", deletedCount, userId);
        return deletedCount;
    }

    // ============================================================================
    // TOKEN QUERIES
    // ============================================================================

    @Override
    @Transactional(readOnly = true)
    public List<RefreshToken> findAllUserTokens(Long userId) {
        log.debug("Finding all refresh tokens for user ID: {}", userId);
        return refreshTokenRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefreshToken> findValidUserTokens(Long userId) {
        log.debug("Finding valid refresh tokens for user ID: {}", userId);
        return refreshTokenRepository.findValidTokensByUserId(userId, Instant.now());
    }

    @Override
    @Transactional(readOnly = true)
    public long countValidUserTokens(Long userId) {
        log.debug("Counting valid refresh tokens for user ID: {}", userId);
        return refreshTokenRepository.countValidTokensByUserId(userId, Instant.now());
    }

    // ============================================================================
    // CLEANUP OPERATIONS
    // ============================================================================

    @Override
    @Transactional
    public int deleteExpiredTokens() {
        log.debug("Deleting expired refresh tokens");

        int deletedCount = refreshTokenRepository.deleteByExpiryDateBefore(Instant.now());

        if (deletedCount > 0) {
            log.info("Deleted {} expired refresh tokens", deletedCount);
        } else {
            log.debug("No expired tokens to delete");
        }

        return deletedCount;
    }

    @Override
    @Transactional
    public int deleteRevokedTokens() {
        log.debug("Deleting revoked refresh tokens");

        int deletedCount = refreshTokenRepository.deleteRevokedTokens();

        if (deletedCount > 0) {
            log.info("Deleted {} revoked refresh tokens", deletedCount);
        } else {
            log.debug("No revoked tokens to delete");
        }

        return deletedCount;
    }

    // ============================================================================
    // REUSE DETECTION
    // ============================================================================

    @Override
    @Transactional
    public void handleTokenReuse(String tokenString, Long userId) {
        log.error("⚠️ SECURITY ALERT: Token reuse detected! Token: {}, User ID: {}",
                maskToken(tokenString), userId);

        // Revoke all user tokens as security measure
        int revokedCount = revokeAllUserTokens(userId);

        log.error("Security response: Revoked {} tokens for user ID: {}", revokedCount, userId);

        // Throw exception with security flag
        throw new RefreshTokenRevokedException(
                maskToken(tokenString),
                "Token reuse detected. All your sessions have been terminated for security. Please login again.",
                true // Security breach flag
        );
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    /**
     * Extract user agent from HTTP request
     */
    private String extractUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && userAgent.length() > 500) {
            userAgent = userAgent.substring(0, 500); // Truncate to fit DB column
        }
        return userAgent;
    }

    /**
     * Extract IP address from HTTP request
     * Handles proxy headers (X-Forwarded-For, X-Real-IP)
     */
    private String extractIpAddress(HttpServletRequest request) {
        // Check proxy headers first
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // X-Forwarded-For can contain multiple IPs (client, proxy1, proxy2, ...)
        // Take the first one (original client IP)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        if (ip != null && ip.length() > 50) {
            ip = ip.substring(0, 50); // Truncate to fit DB column
        }

        return ip;
    }

    /**
     * Mask token string for logging (show only first and last 4 chars)
     * Security: Never log full tokens
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "****";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }
}
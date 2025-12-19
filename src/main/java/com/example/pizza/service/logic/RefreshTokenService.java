package com.example.pizza.service.logic;

import com.example.pizza.entity.token.RefreshToken;
import com.example.pizza.entity.user.User;
import com.example.pizza.exceptions.token.RefreshTokenExpiredException;
import com.example.pizza.exceptions.token.RefreshTokenNotFoundException;
import com.example.pizza.exceptions.token.RefreshTokenRevokedException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Optional;

/**
 * RefreshToken Service Interface
 *
 * Business logic layer for refresh token management.
 * Handles token lifecycle: creation, validation, rotation, revocation, and cleanup.
 *
 *
 * Key Responsibilities:
 * - Create refresh tokens with proper expiration
 * - Validate token expiry and revocation status
 * - Implement token rotation strategy (one-time use)
 * - Revoke tokens on logout or security breach
 * - Clean up expired tokens
 *
 * Security Features:
 * - Token rotation: New token generated on each refresh
 * - Reuse detection: Revokes all user tokens if reuse detected
 * - Device tracking: Stores user agent and IP for audit
 * - Automatic cleanup: Scheduled task removes expired tokens
 */
public interface RefreshTokenService {

    // ============================================================================
    // TOKEN CREATION
    // ============================================================================

    /**
     * Create a new refresh token for a user
     *
     * Creates a new refresh token with:
     * - Unique UUID token string
     * - Expiration based on configuration
     * - Association with user
     * - Device tracking (user agent, IP)
     *
     * @param user - The user to create token for
     * @return Created RefreshToken entity
     */
    RefreshToken createRefreshToken(User user);

    /**
     * Create a new refresh token with request context
     *
     * Includes device tracking from HTTP request:
     * - User agent string
     * - Client IP address
     *
     * @param user - The user to create token for
     * @param request - HTTP request for device tracking
     * @return Created RefreshToken entity
     */
    RefreshToken createRefreshToken(User user, HttpServletRequest request);

    // ============================================================================
    // TOKEN VALIDATION
    // ============================================================================

    /**
     * Find refresh token by token string
     *
     * @param token - The refresh token string (UUID)
     * @return Optional containing RefreshToken if found
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Verify refresh token expiration
     *
     * Checks if token is expired and throws exception if invalid.
     * Updates last_used_at timestamp if valid.
     *
     * @param token - The RefreshToken to verify
     * @return The same token if valid
     * @throws RefreshTokenExpiredException if expired
     */
    RefreshToken verifyExpiration(RefreshToken token);

    /**
     * Validate refresh token
     *
     * Complete validation including:
     * - Token exists in database
     * - Not expired
     * - Not revoked
     * - Updates last used timestamp
     *
     * @param tokenString - The refresh token string
     * @return Valid RefreshToken entity
     * @throws RefreshTokenNotFoundException if not found
     * @throws RefreshTokenExpiredException if expired
     * @throws RefreshTokenRevokedException if revoked
     */
    RefreshToken validateToken(String tokenString);

    // ============================================================================
    // TOKEN ROTATION
    // ============================================================================

    /**
     * Rotate refresh token (one-time use pattern)
     *
     * Token rotation process:
     * 1. Validates old token
     * 2. Deletes old token (one-time use)
     * 3. Creates new token for same user
     * 4. Returns new token
     *
     * Security: If old token is already deleted/revoked, indicates reuse attack.
     *
     * @param oldToken - The old refresh token string
     * @return New RefreshToken entity
     * @throws RefreshTokenNotFoundException if token not found
     * @throws RefreshTokenExpiredException if token expired
     * @throws RefreshTokenRevokedException if token revoked
     */
    RefreshToken rotateToken(String oldToken);

    /**
     * Rotate refresh token with request context
     *
     * Same as rotateToken but includes device tracking for new token.
     *
     * @param oldToken - The old refresh token string
     * @param request - HTTP request for device tracking
     * @return New RefreshToken entity
     */
    RefreshToken rotateToken(String oldToken, HttpServletRequest request);

    // ============================================================================
    // TOKEN REVOCATION
    // ============================================================================

    /**
     * Revoke a specific refresh token
     *
     * Sets revoked flag to true. Used for:
     * - Single session logout
     * - Security incident response
     *
     * @param tokenString - The refresh token string to revoke
     * @return true if token was revoked, false if not found
     */
    boolean revokeToken(String tokenString);

    /**
     * Delete a refresh token (hard delete)
     *
     * Permanently deletes token from database.
     * Used in token rotation (one-time use).
     *
     * @param tokenString - The refresh token string to delete
     * @return true if token was deleted, false if not found
     */
    boolean deleteToken(String tokenString);

    /**
     * Revoke all refresh tokens for a user
     *
     * Sets revoked flag on all user's tokens. Used for:
     * - Logout from all devices
     * - Password change
     * - Security breach response
     *
     * @param userId - The user ID
     * @return Number of tokens revoked
     */
    int revokeAllUserTokens(Long userId);

    /**
     * Delete all refresh tokens for a user (hard delete)
     *
     * Permanently deletes all user's tokens.
     *
     * @param userId - The user ID
     * @return Number of tokens deleted
     */
    int deleteAllUserTokens(Long userId);

    // ============================================================================
    // TOKEN QUERIES
    // ============================================================================

    /**
     * Find all refresh tokens for a user
     *
     * Returns all tokens (valid, expired, revoked).
     * Useful for admin panel or user session management.
     *
     * @param userId - The user ID
     * @return List of all refresh tokens for user
     */
    List<RefreshToken> findAllUserTokens(Long userId);

    /**
     * Find all valid (active) refresh tokens for a user
     *
     * Returns only tokens that are:
     * - Not expired
     * - Not revoked
     *
     * Useful for displaying active sessions to user.
     *
     * @param userId - The user ID
     * @return List of valid refresh tokens
     */
    List<RefreshToken> findValidUserTokens(Long userId);

    /**
     * Count valid refresh tokens for a user
     *
     * Can be used to enforce maximum session limit.
     *
     * @param userId - The user ID
     * @return Number of valid tokens
     */
    long countValidUserTokens(Long userId);

    // ============================================================================
    // CLEANUP OPERATIONS
    // ============================================================================

    /**
     * Delete all expired refresh tokens
     *
     * Called by scheduled cleanup task.
     * Removes tokens past their expiry date.
     *
     * @return Number of tokens deleted
     */
    int deleteExpiredTokens();

    /**
     * Delete all revoked refresh tokens
     *
     * Optional cleanup for revoked tokens.
     * Keeps database clean while maintaining audit trail.
     *
     * @return Number of tokens deleted
     */
    int deleteRevokedTokens();

    // ============================================================================
    // REUSE DETECTION
    // ============================================================================

    /**
     * Handle token reuse detection
     *
     * Called when a deleted/revoked token is attempted to be used.
     * Indicates possible security breach (token theft).
     *
     * Response:
     * 1. Revokes all user tokens
     * 2. Logs security incident
     * 3. Throws exception
     *
     * @param tokenString - The reused token string
     * @param userId - The user ID
     * @throws RefreshTokenRevokedException with security flag
     */
    void handleTokenReuse(String tokenString, Long userId);
}
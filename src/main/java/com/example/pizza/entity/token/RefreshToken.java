package com.example.pizza.entity.token;

import com.example.pizza.entity.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * RefreshToken Entity
 *
 * Stores refresh tokens in database for long-term authentication.
 * Supports token rotation, revocation, and cleanup strategies.
 *
 * Features:
 * - One-to-Many relationship with User
 * - Token expiration tracking
 * - Revocation support for security
 * - Automatic timestamps with JPA Auditing
 *
 * Security Considerations:
 * - Tokens are stored as plain strings (UUID-based)
 * - Can be revoked at any time (logout, security breach)
 * - Expired tokens are cleaned up via scheduled task
 * - One-time use enforced by deletion after refresh
 */
@Entity
@Table(
        schema = "pizza",
        name = "refresh_tokens",
        indexes = {
                @Index(name = "idx_refresh_token", columnList = "token"),
                @Index(name = "idx_refresh_token_user_id", columnList = "user_id"),
                @Index(name = "idx_refresh_token_expiry", columnList = "expiry_date")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false, unique = true, length = 255)
    private String token;

    /**
     * User relationship
     * - Many refresh tokens can belong to one user
     * - FetchType.LAZY for performance (only load when needed)
     * - Cascade removed: tokens are managed independently
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Token expiration timestamp
     * - Uses Instant for UTC time consistency
     * - Checked before each token refresh
     * - Cleanup job uses this field to delete expired tokens
     */
    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    /**
     * Revocation flag
     * - True if token was manually revoked (logout, security)
     * - Revoked tokens cannot be used for refresh
     * - Used for security audit trail
     */
    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private boolean revoked = false;

    /**
     * Creation timestamp
     * - Auto-populated by JPA Auditing
     * - Used for security auditing and analytics
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last used timestamp
     * - Updated each time token is used for refresh
     * - Helps detect suspicious activity (multiple uses)
     */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    /**
     * User agent string (optional)
     * - Stores device/browser information
     * - Useful for security monitoring
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * IP address (optional)
     * - Stores client IP for security tracking
     * - Can be used to detect token theft
     */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    // ============================================================================
    // BUSINESS METHODS
    // ============================================================================

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiryDate);
    }

    public boolean isValid() {
        return !isExpired() && !revoked;
    }

    /**
     * Revoke this token (logout or security breach)
     */
    public void revoke() {
        this.revoked = true;
    }

    public void updateLastUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }
}
package com.example.pizza.config.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.pizza.constants.security.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Unified Token Provider - Handles Supabase and Application JWT tokens
 * - Access Token & Refresh Token Separation
 * - Access Token: Short-lived (30 minutes) for API authentication
 * - Refresh Token: Handled by RefreshTokenService (database-based)
 * - Backward compatible with existing token generation
 */
@Slf4j
@Component
public class UnifiedTokenProvider {

    private final JwksService jwksService;  // Can be null in dev environment

    /**
     * JWT secret key for signing tokens
     * Loaded from: jwt.secret property
     */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * DEPRECATED: Use accessTokenExpiration instead
     * Kept for backward compatibility
     *
     * Phase 4.4: This is now only used by legacy generateToken() methods
     */
    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    /**
     * Access token expiration (short-lived)
     * Default: 1800000ms = 30 minutes
     *
     * Access tokens are short-lived for security.
     * Users must refresh using refresh token when expired.
     */
    @Value("${jwt.access-token.expiration:1800000}")
    private long accessTokenExpiration;

    /**
     * Refresh token expiration (long-lived)
     * Default: 604800000ms = 7 days
     *
     * Note: Refresh tokens are stored in database via RefreshTokenService
     * This value should match jwt.refresh-expiration
     */
    @Value("${jwt.refresh-token.expiration:604800000}")
    private long refreshTokenExpiration;

    @Value("${app.supabase.jwt.secret:}")
    private String supabaseJwtSecret;

    @Value("${app.supabase.enabled:true}")
    private boolean supabaseEnabled;

    /**
     * CRITICAL FIX: Manual constructor to handle optional JwksService
     *
     * @Autowired(required = false) sadece field injection'da çalışır TODO Autowired Deprecated bir lombok annotation olacak.
     * Constructor injection için manuel constructor gerekli
     */
    public UnifiedTokenProvider(@Autowired(required = false) JwksService jwksService) {
        this.jwksService = jwksService;
        if (jwksService == null) {
            log.info("JwksService not available - Supabase JWT validation disabled (dev mode)");
        } else {
            log.info("JwksService available - Supabase JWT validation enabled (prod mode)");
        }
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ============================================================================
    // PHASE 4.4: NEW ACCESS TOKEN GENERATION
    // ============================================================================

    /**
     * Generate Access Token (short-lived, 30 minutes)
     *
     * Primary method for generating access tokens
     *
     * Access Token Properties:
     * - Short expiration (30 minutes)
     * - Contains user email and roles
     * - Type claim: "access"
     * - Used for API authentication
     * - Cannot be refreshed directly
     *
     * @param email - User email
     * @param authorities - User roles/authorities
     * @return JWT access token string
     */
    public String generateAccessToken(String email, Collection<? extends GrantedAuthority> authorities) {
        log.debug("Generating access token for user: {}", email);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);

        List<String> roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        String token = Jwts.builder()
                .setSubject(email)
                .claim("roles", roles)
                .claim("type", TokenType.ACCESS.getValue())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey())
                .compact();

        log.info("Access token generated for user: {} (expires in {} minutes)",
                email, accessTokenExpiration / 60000);

        return token;
    }

    /**
     * Generate Access Token from Authentication object
     *
     * Convenience method for Spring Security Authentication
     *
     * @param authentication - Spring Security Authentication
     * @return JWT access token string
     */
    public String generateAccessToken(Authentication authentication) {
        String email = authentication.getName();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return generateAccessToken(email, authorities);
    }

    // ============================================================================
    // LEGACY TOKEN GENERATION (Backward Compatibility)
    // ============================================================================

    /**
     * DEPRECATED: Use generateAccessToken() instead
     *
     * Legacy method kept for backward compatibility.
     * Uses old jwt.expiration configuration (typically 7 days).
     *
     * Phase 4.4: This method is deprecated but still functional.
     * New code should use generateAccessToken() for short-lived tokens.
     *
     * @deprecated Use {@link #generateAccessToken(String, Collection)} instead
     */
    @Deprecated
    public String generateToken(String email, Collection<? extends GrantedAuthority> authorities) {
        log.warn("Using deprecated generateToken() method. Consider using generateAccessToken() instead.");

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        List<String> roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .setSubject(email)
                .claim("roles", roles)
                .claim("type", "application")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * DEPRECATED: Use generateAccessToken(Authentication) instead
     *
     * @deprecated Use {@link #generateAccessToken(Authentication)} instead
     */
    @Deprecated
    public String generateToken(Authentication authentication) {
        log.warn("Using deprecated generateToken() method. Consider using generateAccessToken() instead.");
        String email = authentication.getName();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return generateToken(email, authorities);
    }

    // ============================================================================
    // TOKEN VALIDATION
    // ============================================================================

    /**
     * Validate and parse JWT token
     *
     * Supports:
     * - Application JWT tokens (access tokens)
     * - Supabase OAuth tokens
     *
     * Works with both access tokens and legacy tokens
     *
     * @param token - JWT token string
     * @return Claims object containing token data
     * @throws RuntimeException if token is invalid
     */
    public Claims validateToken(String token) {
        try {
            // Try application JWT first (most common)
            return validateApplicationToken(token);
        } catch (Exception e) {
            log.debug("Application JWT validation failed, trying Supabase JWT", e);

            // Only try Supabase validation if enabled and JwksService is available
            if (supabaseEnabled && jwksService != null) {
                try {
                    return validateSupabaseToken(token);
                } catch (Exception supabaseEx) {
                    log.error("Both application and Supabase JWT validation failed", supabaseEx);
                    throw new RuntimeException("Invalid token", supabaseEx);
                }
            } else {
                log.debug("Supabase JWT validation skipped (disabled or JwksService not available)");
                throw new RuntimeException("Invalid application JWT token", e);
            }
        }
    }

    /**
     * Validate application-generated JWT
     *
     * Validates access tokens and legacy tokens.
     * Checks signature, expiration, and structure.
     *
     * @param token - JWT token string
     * @return Claims object
     */
    private Claims validateApplicationToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Validate Supabase-generated JWT
     *
     * Only called if Supabase is enabled and JwksService exists
     */
    private Claims validateSupabaseToken(String token) {
        if (jwksService == null) {
            throw new RuntimeException("JwksService not available - Supabase JWT validation disabled");
        }

        try {
            DecodedJWT decodedJWT = JWT.decode(token);
            String kid = decodedJWT.getKeyId();

            if (kid == null) {
                throw new RuntimeException("Token missing 'kid' (Key ID)");
            }

            RSAPublicKey publicKey = jwksService.getPublicKey(kid);
            Algorithm algorithm = Algorithm.RSA256(publicKey, null);

            JWT.require(algorithm)
                    .build()
                    .verify(token);

            // Convert to Claims format
            return convertSupabaseToApplicationClaims(decodedJWT);

        } catch (Exception e) {
            log.error("Supabase JWT validation failed", e);
            throw new RuntimeException("Invalid Supabase token", e);
        }
    }

    /**
     * Convert Supabase JWT claims to application Claims format
     */
    private Claims convertSupabaseToApplicationClaims(DecodedJWT jwt) {
        Claims claims = Jwts.claims();

        claims.setSubject(jwt.getClaim("email").asString());
        claims.setIssuer(jwt.getIssuer());
        claims.setExpiration(jwt.getExpiresAt());
        claims.setNotBefore(jwt.getNotBefore());
        claims.setIssuedAt(jwt.getIssuedAt());
        claims.setId(jwt.getId());

        String role = jwt.getClaim("user_role").asString();
        claims.put("roles", role != null ?
                List.of("ROLE_" + role.toUpperCase()) : List.of("ROLE_CUSTOMER"));
        claims.put("type", "supabase");

        return claims;
    }

    // ============================================================================
    // TOKEN UTILITY METHODS
    // ============================================================================

    /**
     * Extract email from token
     *
     * Works with access tokens, legacy tokens, and Supabase tokens.
     *
     * @param token - JWT token string
     * @return User email
     */
    public String getEmailFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.getSubject();
    }

    /**
     * Extract authorities from token
     *
     * Parses roles claim and converts to Spring Security authorities.
     *
     * @param token - JWT token string
     * @return Collection of authorities
     */
    public Collection<SimpleGrantedAuthority> getAuthoritiesFromToken(String token) {
        Claims claims = validateToken(token);
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles");

        if (roles == null || roles.isEmpty()) {
            return List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
        }

        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    /**
     * Check if token is access token (vs legacy or Supabase token)
     *
     * Helper method to identify token type
     *
     * @param token - JWT token string
     * @return true if token is access token
     */
    public boolean isAccessToken(String token) {
        try {
            Claims claims = validateToken(token);
            String type = (String) claims.get("type");
            return TokenType.ACCESS.getValue().equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get token type from token
     *
     * Returns token type (access, application, supabase)
     *
     * @param token - JWT token string
     * @return Token type string
     */
    public String getTokenType(String token) {
        try {
            Claims claims = validateToken(token);
            return (String) claims.get("type");
        } catch (Exception e) {
            return "unknown";
        }
    }

    // ============================================================================
    // GETTERS FOR CONFIGURATION (Testing & Monitoring)
    // ============================================================================

    /**
     * Get access token expiration time in milliseconds
     * Useful for frontend to calculate token refresh timing
     */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    /**
     * Get refresh token expiration time in milliseconds
     * Useful for frontend to display session duration
     */
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    /**
     * DEPRECATED: Get legacy token expiration
     *
     * @deprecated Use getAccessTokenExpiration() instead
     */
    @Deprecated
    public long getTokenExpiration() {
        return jwtExpiration;
    }
}
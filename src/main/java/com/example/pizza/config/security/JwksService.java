package com.example.pizza.config.security;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.security.interfaces.RSAPublicKey;

/**
 * JWKS (JSON Web Key Set) Service
 *
 * Supabase'den public key'leri alır ve JWT validation için kullanır.
 *
 * CONDITIONAL: Sadece app.supabase.enabled=true olduğunda aktif
 *
 * Dev ortamında: Bu service devre dışı (app.supabase.enabled=false)
 * Prod ortamında: Bu service aktif (app.supabase.enabled=true)
 */
@Slf4j
@Service
@ConditionalOnProperty(
        prefix = "app.supabase",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true  // Default: true (backward compatibility için)
)
public class JwksService {

    private final JwkProvider jwkProvider;

    public JwksService(@Value("${app.supabase.url}") String supabaseUrl) {
        try {
            String jwksUrl = supabaseUrl + "/auth/v1/jwks";
            log.info("Initializing JWKS provider with URL: {}", jwksUrl);
            this.jwkProvider = new UrlJwkProvider(new URL(jwksUrl));
            log.info("JWKS Service initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize JWKS provider", e);
            throw new RuntimeException("JWKS initialization failed", e);
        }
    }

    public RSAPublicKey getPublicKey(String kid) {
        try {
            Jwk jwk = jwkProvider.get(kid);
            return (RSAPublicKey) jwk.getPublicKey();
        } catch (Exception e) {
            log.error("Failed to get public key for kid: {}", kid, e);
            throw new RuntimeException("Failed to get public key", e);
        }
    }
}
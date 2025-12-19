package com.example.pizza.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class UnifiedAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(UnifiedAuthenticationFilter.class);

    private final UnifiedTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    @Autowired
    public UnifiedAuthenticationFilter(UnifiedTokenProvider tokenProvider, UserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        logger.info("UnifiedAuthenticationFilter initialized");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);
            String requestPath = request.getRequestURI();

            logger.debug("Processing request: {}", requestPath);

            // Token kontrolü ve security context kontrolü
            if (StringUtils.hasText(jwt) && SecurityContextHolder.getContext().getAuthentication() == null) {
                logger.debug("JWT token found in request, and no authentication exists in context");

                try {
                    // validateToken will throw an exception if invalid
                    tokenProvider.validateToken(jwt);
                    String email = tokenProvider.getEmailFromToken(jwt);

                    if (email != null) {
                        logger.debug("Email extracted from token: {}", email);

                        try {
                            // Kullanıcı detaylarını yükle
                            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                            // Authentication nesnesi oluştur
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails, null, userDetails.getAuthorities());

                            // Details'ı ayarla
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                            // Context'e authentication'ı ayarla
                            SecurityContextHolder.getContext().setAuthentication(authentication);

                            logger.debug("User '{}' authenticated successfully with roles: {}",
                                    email, userDetails.getAuthorities());
                        } catch (Exception e) {
                            logger.warn("Error loading user details for email: {}", email, e);
                        }
                    } else {
                        logger.warn("Could not extract email from token");
                    }
                } catch (RuntimeException e) { // Catch the RuntimeException thrown by validateToken
                    logger.debug("Invalid JWT token or validation failed: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Authentication error: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
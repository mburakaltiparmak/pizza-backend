package com.example.pizza.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;
    private final UnifiedTokenProvider tokenProvider;

    @Bean
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(auth -> auth
                        // ================================================================
                        // 1. PUBLIC ENDPOINTS (AUTH, SYSTEM, UPLOADS)
                        // ================================================================
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/upload/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()

                        // ================================================================
                        // 2. ELASTICSEARCH REINDEXING (ADMIN ONLY)
                        // ================================================================
                        // Spesifik kurallar wildcard kurallardan önce yazılmalıdır. SecurityConfig'de sıralama önemli.
                        .requestMatchers(HttpMethod.POST, "/api/product/reindex").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/category/reindex").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/users/reindex").hasRole("ADMIN")

                        // ================================================================
                        // 3. SEARCH ENDPOINTS
                        // ================================================================

                        // Product & Category Search -> PUBLIC
                        .requestMatchers(HttpMethod.GET, "/api/product/search/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/product/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/category/search/**").permitAll()

                        // User Search (Admin Panel) -> ADMIN ONLY
                        .requestMatchers(HttpMethod.GET, "/api/admin/users/search/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/users/filter").hasRole("ADMIN")

                        // Phase 6: Search Suggestions (Autocomplete, Fuzzy) -> PUBLIC
                        .requestMatchers(HttpMethod.GET, "/api/search/**").permitAll()

                        // ================================================================
                        // 4. PRODUCT & CATEGORY
                        // ================================================================
                        // Public GET access for listings
                        .requestMatchers(HttpMethod.GET, "/api/product/**", "/api/category/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/product/custom-pizza").permitAll()

                        // Management (Create/Update -> Admin or Personal, Delete -> Admin)
                        .requestMatchers(HttpMethod.POST, "/api/product/**", "/api/category/**")
                        .hasAnyRole("ADMIN", "PERSONAL")
                        .requestMatchers(HttpMethod.PUT, "/api/product/**", "/api/category/**")
                        .hasAnyRole("ADMIN", "PERSONAL")
                        .requestMatchers(HttpMethod.DELETE, "/api/product/**", "/api/category/**").hasRole("ADMIN")

                        // ================================================================
                        // 5. PAYMENT ENDPOINTS
                        // ================================================================
                        // 3DS callback MUST be public (called by Iyzico)
                        .requestMatchers(HttpMethod.POST, "/api/payment/3ds/callback").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payment/3ds/callback/json").permitAll()
                        // Payment processing requires authentication
                        .requestMatchers("/api/payment/*/refund", "/api/payment/*/cancel").hasRole("ADMIN")
                        .requestMatchers("/api/payment/**").authenticated()

                        // ================================================================
                        // 6. ORDER MANAGEMENT
                        // ================================================================
                        .requestMatchers(HttpMethod.POST, "/api/orders").permitAll() // Guest orders allowed
                        .requestMatchers(HttpMethod.POST, "/api/orders/*/pay/**").permitAll()
                        .requestMatchers("/api/orders/**").authenticated()

                        // ================================================================
                        // 7. USER & ADDRESS MANAGEMENT
                        // ================================================================
                        .requestMatchers("/api/user/addresses/**").authenticated()
                        .requestMatchers("/api/addresses/**").authenticated()
                        .requestMatchers("/api/user/**").hasAnyRole("ADMIN", "PERSONAL", "CUSTOMER")

                        // ================================================================
                        // 8. ADMIN GENERIC
                        // ================================================================
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // ================================================================
                        // 9. FALLBACK
                        // ================================================================
                        .anyRequest().authenticated())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(unifiedAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public UnifiedAuthenticationFilter unifiedAuthenticationFilter() {
        return new UnifiedAuthenticationFilter(tokenProvider, userDetailsService);
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(List.of(authenticationProvider()));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
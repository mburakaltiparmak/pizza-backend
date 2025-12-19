package com.example.pizza.config.actuator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Order(1) // Ana SecurityConfig'den önce çalışması için
public class ActuatorSecurityConfig {

    @Bean
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                // Sadece actuator endpoint'lerine uygulanır
                .securityMatcher("/actuator/**")
                .authorizeHttpRequests(auth -> auth
                                // Health ve info endpoint'leri herkese açık
                                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                                .requestMatchers("/actuator/info").permitAll()

                                // Metrik ve istatistik endpoint'leri - portfolio için açık
                                .requestMatchers("/actuator/metrics", "/actuator/metrics/**").permitAll()
                                .requestMatchers("/actuator/app-stats", "/actuator/app-stats/**").permitAll()
                                .requestMatchers("/actuator/httpexchanges").permitAll()

                                // Cache management - demo için açık
                                .requestMatchers("/actuator/cache-management/**").permitAll()

                                // Diğer tüm actuator endpoint'leri - production'da kapalı olmalı
                                .requestMatchers("/actuator/**").permitAll()

                        // NOT: Production ortamında yukarıdaki permitAll() yerine
                        // .hasRole("ADMIN") veya .authenticated() kullanılmalıdır
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
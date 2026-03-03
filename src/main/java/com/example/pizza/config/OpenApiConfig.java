package com.example.pizza.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger Configuration
 * 
 * Provides interactive API documentation via Swagger UI.
 * Access: http://localhost:8080/pizza/swagger-ui.html
 * 
 * @author Burak Altıparmak
 * @version 2.0.0
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pizzaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Pizza Order System API")
                        .description("Enterprise-grade Pizza Order Management System with JWT authentication, " +
                                "Redis caching, Elasticsearch search, and Iyzico payment integration")
                        .version("2.0.0")
                        .contact(new Contact()
                                .name("Burak Altıparmak")
                                .email("info@burakaltiparmak.site")
                                .url("https://burakaltiparmak.site"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .externalDocs(new ExternalDocumentation()
                        .description("GitHub Repository")
                        .url("https://github.com/mburakaltiparmak/pizza"))
                .addSecurityItem(new SecurityRequirement()
                        .addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .name("Bearer Authentication")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token obtained from /api/auth/login endpoint. " +
                                                "Login with email and password to get the token.")));
    }
}

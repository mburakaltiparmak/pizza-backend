package com.example.pizza.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class AppConfig {

    @Value("${cloudinary.cloud-name:${CLOUD_NAME:}}")
    private String cloudName;

    @Value("${cloudinary.api-key:${API_KEY:}}")
    private String apiKey;

    @Value("${cloudinary.api-secret:${API_SECRET:}}")
    private String apiSecret;

}
package com.example.pizza.config.logic;

import com.cloudinary.Cloudinary;
import com.cloudinary.api.ApiResponse;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CloudinaryConfig {
    @Value("${cloudinary.cloud-name:${CLOUD_NAME:dqjqkgpt3}}")
    private String cloudName;

    @Value("${cloudinary.api-key:${API_KEY:526785153986271}}")
    private String apiKey;

    @Value("${cloudinary.api-secret:${API_SECRET:jJcztrT-qS5BpgQOVpdbMGDwXdY}}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", cloudName);
        config.put("api_key", apiKey);
        config.put("api_secret", apiSecret);

        Cloudinary cloudinary = new Cloudinary(config);

        // Cloudinary bağlantısını test et
        try {
            ApiResponse apiResponse = cloudinary.api().createFolder("pizza", ObjectUtils.emptyMap());
            System.out.println("Cloudinary bağlantısı başarılı: " + apiResponse);
        } catch (Exception e) {
            System.err.println("Cloudinary bağlantısı başarısız: " + e.getMessage());
            throw new RuntimeException("Cloudinary yapılandırması başarısız oldu", e);
        }

        return cloudinary;
    }
}
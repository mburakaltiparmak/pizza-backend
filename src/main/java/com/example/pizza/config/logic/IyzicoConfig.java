package com.example.pizza.config.logic;

import com.iyzipay.Options;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "iyzico")
@Getter
@Setter
public class IyzicoConfig {
    private String apiKey;
    private String secretKey;
    private String baseUrl;
    private String callbackUrl;

    @Bean
    public Options iyzicoOptions() {
        Options options = new Options();
        options.setApiKey(apiKey);
        options.setSecretKey(secretKey);
        options.setBaseUrl(baseUrl);
        return options;
    }

    @javax.annotation.PostConstruct
    public void validateConfig() {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException(
                    "Iyzico API Key (IYZICO_API_KEY) tanımlanmamış! Lütfen environment variables veya .env dosyasını kontrol edin.");
        }
        if (secretKey == null || secretKey.isEmpty()) {
            throw new IllegalStateException("Iyzico Secret Key (IYZICO_SECRET_KEY) tanımlanmamış!");
        }
        System.out.println("✅ Iyzico Config loaded. URL: " + baseUrl + ", API Key: " + apiKey.substring(0, 4) + "***");
    }
}

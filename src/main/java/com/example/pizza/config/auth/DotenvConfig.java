package com.example.pizza.config.auth;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class DotenvConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();

        // Check if we're in dev profile (or no profile set yet)
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isDevProfile = activeProfiles.length == 0 ||
                java.util.Arrays.asList(activeProfiles).contains("dev");

        if (!isDevProfile) {
            log.debug("Skipping .env loading (not dev profile)");
            return;
        }

        try {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing() // Don't fail if .env is missing
                    .load();

            // Add all .env entries to Spring Environment
            Map<String, Object> dotenvMap = new HashMap<>();
            dotenv.entries().forEach(entry -> {
                dotenvMap.put(entry.getKey(), entry.getValue());
            });

            environment.getPropertySources()
                    .addFirst(new MapPropertySource("dotenvProperties", dotenvMap));

            log.debug(".env file loaded successfully ({} variables)", dotenvMap.size());

        } catch (Exception e) {
            log.warn("Warning: Could not load .env file: {}", e.getMessage());
            log.warn("Make sure .env file exists in project root");
            log.warn("Continuing with system environment variables...");
        }
    }
}
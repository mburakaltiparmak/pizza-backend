package com.example.pizza.config;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.protocol.JacksonJsonSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SocketIOConfig {

    @Bean
    public SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();

        // PROPER BINDING FOR DOCKER
        config.setHostname("0.0.0.0");
        config.setPort(9092);

        // CORS Configuration
        config.setOrigin("*"); // Allow all for now, dev environment

        // Configure Jackson with JSR310 support by extending JacksonJsonSupport
        JacksonJsonSupport jsonSupport = new JacksonJsonSupport() {
            @Override
            protected void init(ObjectMapper objectMapper) {
                super.init(objectMapper);
                objectMapper.registerModule(new JavaTimeModule());
            }
        };
        config.setJsonSupport(jsonSupport);

        return new SocketIOServer(config);
    }
}

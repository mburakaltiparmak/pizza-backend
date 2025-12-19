package com.example.pizza.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticationResponse {

    private String accessToken;
    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";
    private Long expiresIn;

    @Deprecated
    private String token;

    @Deprecated
    public AuthenticationResponse(String token) {
        this.token = token;
        this.accessToken = token;
        this.tokenType = "Bearer";
    }
}
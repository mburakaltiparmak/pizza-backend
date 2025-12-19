package com.example.pizza.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/api/auth")
public class OAuth2RedirectController {

    @Value("${app.supabase.url}")
    private String supabaseUrl;

    @GetMapping("/google")
    public RedirectView redirectToSupabaseGoogleAuth() {
        // Redirect to Supabase Google auth endpoint
        RedirectView redirectView = new RedirectView();
        redirectView.setUrl(supabaseUrl + "/auth/v1/authorize?provider=google");
        return redirectView;
    }

    @GetMapping("/oauth2/callback/google")
    public RedirectView handleGoogleCallback() {
        // This should redirect to your frontend which will handle the token
        RedirectView redirectView = new RedirectView();
        redirectView.setUrl("http://localhost:3000/oauth2/callback");
        return redirectView;
    }
}
package com.example.pizza.controller;

import com.example.pizza.dto.user.SupabaseUserDto;
import com.example.pizza.entity.user.User;
import com.example.pizza.service.user.SupabaseUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhook/supabase")
public class SupabaseWebhookController {
    private static final Logger logger = LoggerFactory.getLogger(SupabaseWebhookController.class);

    private final SupabaseUserService supabaseUserService;

    @Autowired
    public SupabaseWebhookController(SupabaseUserService supabaseUserService) {
        this.supabaseUserService = supabaseUserService;
    }

    @PostMapping("/user-event")
    public ResponseEntity<?> handleUserEvent(@RequestBody SupabaseUserDto supabaseUser,
                                             @RequestHeader("X-Supabase-Webhook-Secret") String webhookSecret) {
        // Webhook secret kontrolü yapılabilir
        // if (!isValidWebhookSecret(webhookSecret)) {
        //     return ResponseEntity.status(401).body("Unauthorized");
        // }

        try {
            logger.info("Received user event from Supabase for: {}", supabaseUser.getEmail());

            // Kullanıcıyı senkronize et
            User syncedUser = supabaseUserService.syncSupabaseUser(supabaseUser);

            return ResponseEntity.ok().body(Map.of(
                    "status", "success",
                    "message", "User synchronized successfully",
                    "userId", syncedUser.getId()
            ));
        } catch (Exception e) {
            logger.error("Error processing Supabase webhook", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/user-deleted")
    public ResponseEntity<?> handleUserDeleted(@RequestBody Map<String, String> payload,
                                               @RequestHeader("X-Supabase-Webhook-Secret") String webhookSecret) {
        // İşlem burada yapılabilir
        return ResponseEntity.ok().body(Map.of("status", "success"));
    }

}
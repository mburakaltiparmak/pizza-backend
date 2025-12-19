package com.example.pizza.entity.token;

import com.example.pizza.entity.user.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(schema = "pizza",name = "verification_tokens")
public class VerificationToken {
    @jakarta.persistence.Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime expiryDate;
    private boolean enabled;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
}
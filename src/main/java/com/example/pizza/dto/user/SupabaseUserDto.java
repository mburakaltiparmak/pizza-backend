package com.example.pizza.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupabaseUserDto {
    private String supabaseId;
    private String email;
    private String name;
    private String surname;
    private String phoneNumber;
    private String avatarUrl;
    // İhtiyaca göre diğer alanlar eklenebilir
}
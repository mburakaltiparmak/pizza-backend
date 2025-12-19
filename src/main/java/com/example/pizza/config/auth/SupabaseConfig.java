package com.example.pizza.config.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Supabase Configuration - Conditional Enable/Disable
 *
 * Bu configuration, Supabase bağlantısını app.supabase.enabled property'sine göre
 * aktif/deaktif eder.
 *
 * Dev ortamında: app.supabase.enabled=false → JwksService devre dışı
 * Prod ortamında: app.supabase.enabled=true → JwksService aktif
 */
@Configuration
@ConditionalOnProperty(
        prefix = "app.supabase",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true  // Default: true (backward compatibility)
)
public class SupabaseConfig {

    // Bu configuration aktifse, JwksService bean'i oluşturulur
    // Deaktifse, JwksService bean'i oluşturulmaz

    // Notlar:
    // 1. matchIfMissing=true: Property tanımlı değilse default olarak aktif (prod için)
    // 2. Dev ortamında app.supabase.enabled=false olduğu için deaktif
    // 3. Bu sayede JwksService sadece Supabase kullanıldığında çalışır
}
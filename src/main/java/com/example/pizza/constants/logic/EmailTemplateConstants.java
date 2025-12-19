package com.example.pizza.constants.logic;

public enum EmailTemplateConstants {
    ORDER_CONFIRMATION_SUBJECT("Siparişiniz Alındı - #{orderNumber}", "Sipariş onay email başlığı"),
    ORDER_STATUS_SUBJECT("Sipariş Durumu Güncellendi - #{orderNumber}", "Sipariş durum email başlığı"),
    VERIFICATION_SUBJECT("Email Adresinizi Doğrulayın", "Email doğrulama başlığı"),
    PASSWORD_RESET_SUBJECT("Şifre Sıfırlama Talebi", "Şifre sıfırlama başlığı");

    private final String template;
    private final String description;

    EmailTemplateConstants(String template, String description) {
        this.template = template;
        this.description = description;
    }

    public String getTemplate() {
        return template;
    }

    public String getDescription() {
        return description;
    }

    public String format(String... replacements) {
        String result = template;
        // Basit bir placeholder mekanizması
        for (int i = 0; i < replacements.length; i++) {
            // İlk parametreyi orderNumber olarak, diğerlerini paramX olarak varsayalım.
            // Daha gelişmiş bir çözüm için Map kullanmak daha iyi olabilir.
            result = result.replace("#{" + (i == 0 ? "orderNumber" : "param" + i) + "}", replacements[i]);
        }
        return result;
    }
}
package com.example.pizza.constants.order;

public enum PaymentStatus {
    PENDING, // Beklemede
    PENDING_3DS, // 3D Secure bekliyor
    SUCCESS, // Başarılı
    FAILED, // Başarısız
    REFUNDED, // İade edildi
    CANCELLED // İptal edildi
}
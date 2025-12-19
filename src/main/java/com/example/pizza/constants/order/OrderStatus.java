package com.example.pizza.constants.order;

public enum OrderStatus {
    PENDING,    // Siparişiniz alındı, ödeme bekleniyor
    CONFIRMED,  // Siparişiniz onaylandı, hazırlanıyor
    PREPARING,  // Siparişiniz hazırlanıyor
    READY,      // Siparişiniz hazır, teslimat bekleniyor
    SHIPPING,   // Siparişiniz yolda
    DELIVERED,  // Siparişiniz teslim edildi
    CANCELLED   // Sipariş iptal edildi
}
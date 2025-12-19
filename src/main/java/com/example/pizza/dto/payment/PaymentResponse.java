package com.example.pizza.dto.payment;

import com.example.pizza.constants.order.PaymentMethod;
import com.example.pizza.constants.order.PaymentStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    // ==========================================
    // LEGACY FIELDS (for OrderMapper compatibility)
    // ==========================================
    private Long id;                    // Payment entity ID
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;

    // ==========================================
    // COMMON FIELDS
    // ==========================================
    private Long paymentId;             // Alias for id (Iyzico response)
    private String status;              // SUCCESS, FAILED, PENDING, PENDING_3DS (string version)
    private String transactionId;
    private String errorMessage;
    private BigDecimal amount;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    // ==========================================
    // IYZICO SPECIFIC FIELDS
    // ==========================================
    private String iyzicoPaymentId;
    private String authCode;
    private BigDecimal paidAmount;
    private BigDecimal merchantPayoutAmount; // Net amount after commission
    private String currency;
    private String cardAssociation;
    private String cardFamily;
    private String cardLastFour;
    private Integer installment;
    private String threeDsHtmlContent;  // For 3DS flow
}
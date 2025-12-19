package com.example.pizza.entity.logic;

import com.example.pizza.constants.order.PaymentMethod;
import com.example.pizza.constants.order.PaymentStatus;
import com.example.pizza.entity.order.Order;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(schema = "pizza", name = "payment")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    @JsonBackReference(value = "order-payment")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Order order;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "error_message")
    private String errorMessage;

    // ===========================================
    // IYZICO INTEGRATION FIELDS
    // ===========================================

    @Column(name = "iyzico_payment_id", length = 255)
    private String iyzicoPaymentId;

    @Column(name = "iyzico_conversation_id", length = 255)
    private String iyzicoConversationId;

    @Column(name = "auth_code", length = 50)
    private String authCode;

    @Column(name = "card_association", length = 50)
    private String cardAssociation; // VISA, MASTER_CARD, AMEX

    @Column(name = "card_family", length = 100)
    private String cardFamily; // Bonus, Axess, World

    @Column(name = "card_bin_number", length = 6)
    private String cardBinNumber;

    @Column(name = "card_last_four", length = 4)
    private String cardLastFour;

    @Column(name = "fraud_status")
    private Integer fraudStatus; // 1=OK, 0=FRAUD

    @Column(name = "installment")
    private Integer installment = 1;

    @Column(name = "merchant_commission_rate", precision = 10, scale = 8)
    private BigDecimal merchantCommissionRate;

    @Column(name = "merchant_commission_amount", precision = 10, scale = 2)
    private BigDecimal merchantCommissionAmount;

    @Column(name = "iyzico_commission_rate", precision = 10, scale = 8)
    private BigDecimal iyzicoCommissionRate;

    @Column(name = "iyzico_commission_fee", precision = 10, scale = 2)
    private BigDecimal iyzicoCommissionFee;

    @Column(name = "merchant_payout_amount", precision = 10, scale = 2)
    private BigDecimal merchantPayoutAmount;

    @Column(name = "three_ds_html_content", columnDefinition = "TEXT")
    private String threeDsHtmlContent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
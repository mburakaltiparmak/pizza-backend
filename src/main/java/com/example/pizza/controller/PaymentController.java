package com.example.pizza.controller;

import com.example.pizza.dto.payment.PaymentCardRequest;
import com.example.pizza.dto.payment.PaymentResponse;
import com.example.pizza.entity.logic.Payment;
import com.example.pizza.service.payment.IyzicoPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Payment Controller - Iyzico Integration
 *
 * Endpoints:
 * - POST /api/payment/process/{orderId} - Direct payment (Non-3DS, sandbox
 * only)
 * - POST /api/payment/3ds/init/{orderId} - Initialize 3DS payment
 * - POST /api/payment/3ds/callback - 3DS callback handler (PUBLIC)
 * - GET /api/payment/{paymentId}/status - Get payment status
 * - GET /api/payment/order/{orderId} - Get payment by order ID
 * - POST /api/payment/{paymentId}/refund - Refund payment (ADMIN)
 * - POST /api/payment/{paymentId}/cancel - Cancel payment (ADMIN)
 */
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final IyzicoPaymentService iyzicoPaymentService;
    private final com.example.pizza.service.order.OrderService orderService;

    // =========================================================================
    // DIRECT PAYMENT (Non-3DS) - FOR TESTING ONLY
    // =========================================================================

    /**
     * Process direct payment (Non-3DS)
     * USE ONLY FOR TESTING - 3DS is mandatory for production in Turkey
     *
     * Test Cards:
     * - Success: 5528790000000008
     * - Failure: 5528790000000016
     */
    @PostMapping("/process/{orderUuid}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<PaymentResponse> processDirectPayment(
            @PathVariable java.util.UUID orderUuid,
            @Valid @RequestBody PaymentCardRequest cardRequest) {

        log.info("Direct payment request for order UUID: {}", orderUuid);
        Long orderId = orderService.getOrderByUuid(orderUuid).getId();
        PaymentResponse response = iyzicoPaymentService.processDirectPayment(orderId, cardRequest);

        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(response);
        }
    }

    // =========================================================================
    // 3D SECURE PAYMENT (PRODUCTION RECOMMENDED)
    // =========================================================================

    /**
     * Initialize 3D Secure payment
     * Returns HTML content to render in iframe for 3DS verification
     */
    @PostMapping("/3ds/init/{orderUuid}")
    // @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')") // Allow guests
    public ResponseEntity<PaymentResponse> initThreeDSPayment(
            @PathVariable java.util.UUID orderUuid,
            @Valid @RequestBody PaymentCardRequest cardRequest,
            @RequestParam(required = false) String callbackUrl) {

        log.info("3DS init request for order UUID: {}", orderUuid);
        Long orderId = orderService.getOrderByUuid(orderUuid).getId();

        PaymentResponse response = iyzicoPaymentService.initThreeDSPayment(
                orderId, cardRequest, callbackUrl);

        if ("PENDING_3DS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else if ("FAILED".equals(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Initialize Iyzico Checkout Form (Hosted Payment Page)
     * Returns script/HTML only - sensitive card data is collected by Iyzico
     */
    @PostMapping("/checkout/init/{orderUuid}")
    // @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')") // Allow guests
    public ResponseEntity<PaymentResponse> initCheckoutForm(
            @PathVariable java.util.UUID orderUuid,
            @RequestParam(required = false) String callbackUrl) {

        log.info("Checkout form init request for order UUID: {}", orderUuid);
        Long orderId = orderService.getOrderByUuid(orderUuid).getId();

        PaymentResponse response = iyzicoPaymentService.initCheckoutForm(orderId, callbackUrl);

        if ("PENDING_CHECKOUT".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else if ("FAILED".equals(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(response);
        }

        return ResponseEntity.ok(response);
    }

    @Value("${app.frontend.url}")
    private String frontendUrl;

    // ...

    /**
     * 3D Secure callback endpoint
     * This is called by Iyzico after user completes 3DS verification
     * MUST BE PUBLIC - No authentication required
     */
    @PostMapping("/3ds/callback")
    public ResponseEntity<?> handleThreeDSCallback(
            @RequestParam(required = false) String conversationId,
            @RequestParam(required = false) String paymentId,
            @RequestParam(required = false) String token,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String mdStatus) {

        log.info("Callback received - token: {}, conversationId: {}, paymentId: {}, status: {}",
                token, conversationId, paymentId, status);

        try {
            PaymentResponse response;

            if (token != null) {
                // Return HTML page that redirects to success page
                response = iyzicoPaymentService.handleCheckoutCallback(token);
            } else if (conversationId != null && paymentId != null) {
                // Return HTML page that redirects to failure page
                response = iyzicoPaymentService.handleThreeDSCallback(conversationId, paymentId);
            } else {
                log.error("Missing required callback parameters");
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Missing required parameters",
                        "message", "token or conversationId+paymentId required"));
            }

            log.info("Callback Response - OrderUUID: {}, OrderID: {}, Status: {}",
                    response.getOrderUuid(), response.getOrderId(), response.getStatus());

            String targetValue;
            String paramName;

            if (response.getOrderUuid() != null) {
                targetValue = response.getOrderUuid().toString();
                paramName = "uuid";
            } else {
                targetValue = response.getOrderId() != null
                        ? String.valueOf(response.getOrderId())
                        : (response.getIyzicoConversationId() != null
                                ? response.getIyzicoConversationId()
                                : (conversationId != null ? conversationId : "0"));
                paramName = "orderId";
            }

            if ("SUCCESS".equals(response.getStatus())) {
                String successHtml = buildRedirectHtml(
                        frontendUrl + "/payment/success?" + paramName + "=" + targetValue,
                        "Ödeme başarılı! Yönlendiriliyorsunuz...");
                return ResponseEntity.ok()
                        .header("Content-Type", "text/html; charset=UTF-8")
                        .body(successHtml);
            } else {
                String failureHtml = buildRedirectHtml(
                        frontendUrl + "/payment/failure?" + paramName + "=" + targetValue + "&error="
                                + response.getErrorMessage(),
                        "Ödeme başarısız! Yönlendiriliyorsunuz...");
                return ResponseEntity.ok()
                        .header("Content-Type", "text/html; charset=UTF-8")
                        .body(failureHtml);
            }
        } catch (Exception e) {
            log.error("Callback processing failed", e);
            String errorHtml = buildRedirectHtml(
                    frontendUrl + "/payment/failure?error=" + e.getMessage(),
                    "Ödeme işlemi sırasında hata oluştu!");
            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(errorHtml);
        }
    }

    /**
     * Alternative JSON callback endpoint for API integrations
     */
    @PostMapping("/3ds/callback/json")
    public ResponseEntity<PaymentResponse> handleThreeDSCallbackJson(
            @RequestParam String conversationId,
            @RequestParam String paymentId) {

        log.info("3DS JSON callback - conversationId: {}, paymentId: {}", conversationId, paymentId);

        PaymentResponse response = iyzicoPaymentService.handleThreeDSCallback(conversationId, paymentId);

        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(response);
        }
    }

    // =========================================================================
    // PAYMENT STATUS & RETRIEVAL
    // =========================================================================

    /**
     * Get payment status by payment UUID (Secure)
     */
    @GetMapping("/{uuid}/status")
    // @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')") // Allow guests
    public ResponseEntity<PaymentResponse> getPaymentStatus(@PathVariable String uuid) {
        Payment payment = iyzicoPaymentService.getPaymentByUuid(uuid);

        PaymentResponse response = PaymentResponse.builder()
                .paymentId(payment.getId())
                .id(payment.getId())
                .uuid(payment.getUuid())
                .status(payment.getPaymentStatus().name())
                .paymentStatus(payment.getPaymentStatus())
                .paymentMethod(payment.getPaymentMethod())
                .amount(payment.getAmount())
                .transactionId(payment.getTransactionId())
                .iyzicoPaymentId(payment.getIyzicoPaymentId())
                .authCode(payment.getAuthCode())
                .cardAssociation(payment.getCardAssociation())
                .cardFamily(payment.getCardFamily())
                .cardLastFour(payment.getCardLastFour())
                .installment(payment.getInstallment())
                .merchantPayoutAmount(payment.getMerchantPayoutAmount())
                .errorMessage(payment.getErrorMessage())
                .createdAt(payment.getCreatedAt())
                .completedAt(payment.getCompletedAt())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get payment by order ID
     */
    @GetMapping("/order/{orderUuid}")
    // @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')") // Allow guests
    public ResponseEntity<PaymentResponse> getPaymentByOrderUuid(@PathVariable java.util.UUID orderUuid) {
        Long orderId = orderService.getOrderByUuid(orderUuid).getId();
        Payment payment = iyzicoPaymentService.getPaymentByOrderId(orderId);

        PaymentResponse response = PaymentResponse.builder()
                .paymentId(payment.getId())
                .id(payment.getId())
                .uuid(payment.getUuid())
                .status(payment.getPaymentStatus().name())
                .paymentStatus(payment.getPaymentStatus())
                .paymentMethod(payment.getPaymentMethod())
                .amount(payment.getAmount())
                .transactionId(payment.getTransactionId())
                .iyzicoPaymentId(payment.getIyzicoPaymentId())
                .authCode(payment.getAuthCode())
                .cardAssociation(payment.getCardAssociation())
                .cardFamily(payment.getCardFamily())
                .cardLastFour(payment.getCardLastFour())
                .installment(payment.getInstallment())
                .merchantPayoutAmount(payment.getMerchantPayoutAmount())
                .errorMessage(payment.getErrorMessage())
                .createdAt(payment.getCreatedAt())
                .completedAt(payment.getCompletedAt())
                .build();

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // REFUND & CANCEL (ADMIN ONLY)
    // =========================================================================

    /**
     * Refund payment (full or partial)
     */
    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> refundPayment(
            @PathVariable Long paymentId,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam(required = false, defaultValue = "Müşteri talebi") String reason) {

        log.info("Refund request for payment: {}, amount: {}, reason: {}", paymentId, amount, reason);
        PaymentResponse response = iyzicoPaymentService.refundPayment(paymentId, amount, reason);

        if ("REFUNDED".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Cancel payment (before settlement)
     */
    @PostMapping("/{paymentId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> cancelPayment(@PathVariable Long paymentId) {

        log.info("Cancel request for payment: {}", paymentId);
        PaymentResponse response = iyzicoPaymentService.cancelPayment(paymentId);

        if ("CANCELLED".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Build HTML redirect page for 3DS callback
     */
    private String buildRedirectHtml(String redirectUrl, String message) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta http-equiv="refresh" content="2;url=%s">
                    <title>Ödeme İşleniyor</title>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            height: 100vh;
                            margin: 0;
                            background-color: #f5f5f5;
                        }
                        .container {
                            text-align: center;
                            padding: 40px;
                            background: white;
                            border-radius: 10px;
                            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                        }
                        .spinner {
                            border: 4px solid #f3f3f3;
                            border-top: 4px solid #3498db;
                            border-radius: 50%%;
                            width: 40px;
                            height: 40px;
                            animation: spin 1s linear infinite;
                            margin: 20px auto;
                        }
                        @keyframes spin {
                            0%% { transform: rotate(0deg); }
                            100%% { transform: rotate(360deg); }
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="spinner"></div>
                        <h2>%s</h2>
                    </div>
                    <script>
                        setTimeout(function() {
                            window.location.href = '%s';
                        }, 2000);
                    </script>
                </body>
                </html>
                """.formatted(redirectUrl, message, redirectUrl);
    }
}

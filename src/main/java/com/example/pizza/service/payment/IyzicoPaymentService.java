package com.example.pizza.service.payment;

import com.example.pizza.dto.payment.PaymentCardRequest;
import com.example.pizza.dto.payment.PaymentResponse;
import com.example.pizza.entity.logic.Payment;

import java.math.BigDecimal;

public interface IyzicoPaymentService {

    /**
     * Process direct payment (Non-3DS) - FOR TESTING/SANDBOX ONLY
     * In production, 3DS is mandatory for Turkey
     *
     * @param orderId Order ID to process payment for
     * @param cardRequest Card details
     * @return PaymentResponse with result
     */
    PaymentResponse processDirectPayment(Long orderId, PaymentCardRequest cardRequest);

    /**
     * Initialize 3D Secure payment (PRODUCTION RECOMMENDED)
     * Returns HTML content for 3DS iframe
     *
     * @param orderId Order ID
     * @param cardRequest Card details
     * @param callbackUrl URL to redirect after 3DS verification
     * @return PaymentResponse with threeDsHtmlContent
     */
    PaymentResponse initThreeDSPayment(Long orderId, PaymentCardRequest cardRequest, String callbackUrl);

    /**
     * Handle 3DS callback after user completes authentication
     *
     * @param conversationId Our order ID sent to Iyzico
     * @param paymentId Iyzico's payment ID from callback
     * @return PaymentResponse with final result
     */
    PaymentResponse handleThreeDSCallback(String conversationId, String paymentId);

    /**
     * Get payment by ID
     *
     * @param paymentId Payment ID
     * @return Payment entity
     */
    Payment getPaymentById(Long paymentId);

    /**
     * Get payment by Order ID
     *
     * @param orderId Order ID
     * @return Payment entity
     */
    Payment getPaymentByOrderId(Long orderId);

    /**
     * Refund payment (Full or Partial)
     *
     * @param paymentId Payment ID to refund
     * @param amount Amount to refund (null for full refund)
     * @param reason Refund reason
     * @return PaymentResponse with refund result
     */
    PaymentResponse refundPayment(Long paymentId, BigDecimal amount, String reason);

    /**
     * Cancel payment (before settlement)
     *
     * @param paymentId Payment ID to cancel
     * @return PaymentResponse with cancellation result
     */
    PaymentResponse cancelPayment(Long paymentId);
}

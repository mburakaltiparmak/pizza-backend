package com.example.pizza.service.payment;

import com.example.pizza.entity.order.Order;
import com.example.pizza.entity.logic.Payment;
import com.example.pizza.constants.order.PaymentMethod;
import com.example.pizza.constants.order.PaymentStatus;
import com.example.pizza.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Value("${iyzico.api.key:default_api_key}")
    private String apiKey;

    @Value("${iyzico.secret.key:default_secret_key}")
    private String secretKey;

    @Transactional
    public Payment createPayment(Order order, PaymentMethod paymentMethod) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(BigDecimal.valueOf(order.getTotalAmount()));
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment processPayment(Long paymentId, String cardNumber, String expireMonth,
                                  String expireYear, String cvc, String cardHolderName) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Ödeme bulunamadı"));

        try {
            // İyzico API entegrasyonu burada yapılacak
            // Bu örnek sadece yapıyı göstermek için

            /*
            // İyzico entegrasyonu yapıldığında aşağıdaki kod kullanılabilir

            Options options = new Options();
            options.setApiKey(apiKey);
            options.setSecretKey(secretKey);
            options.setBaseUrl("https://sandbox-api.iyzipay.com");

            CreatePaymentRequest request = new CreatePaymentRequest();
            request.setPrice(new BigDecimal(payment.getAmount()));
            request.setPaidPrice(new BigDecimal(payment.getAmount()));
            request.setCurrency(Currency.TRY.name());
            request.setInstallment(1);
            request.setPaymentChannel(PaymentChannel.WEB.name());
            request.setPaymentGroup(PaymentGroup.PRODUCT.name());

            PaymentCard paymentCard = new PaymentCard();
            paymentCard.setCardHolderName(cardHolderName);
            paymentCard.setCardNumber(cardNumber);
            paymentCard.setExpireMonth(expireMonth);
            paymentCard.setExpireYear(expireYear);
            paymentCard.setCvc(cvc);
            request.setPaymentCard(paymentCard);

            // Müşteri bilgileri
            Buyer buyer = new Buyer();
            buyer.setId(payment.getOrder().getUser().getId().toString());
            buyer.setName(payment.getOrder().getUser().getName());
            buyer.setSurname(payment.getOrder().getUser().getSurname());
            buyer.setEmail(payment.getOrder().getUser().getEmail());
            request.setBuyer(buyer);

            // İyzico API çağrısı
            com.iyzipay.model.Payment iyzipayPayment = com.iyzipay.model.Payment.create(request, options);

            if (iyzipayPayment.getStatus().equals("success")) {
                payment.setPaymentStatus(PaymentStatus.SUCCESS);
                payment.setTransactionId(iyzipayPayment.getPaymentId());
                payment.setCompletedAt(LocalDateTime.now());
            } else {
                payment.setPaymentStatus(PaymentStatus.FAILED);
                payment.setErrorMessage(iyzipayPayment.getErrorMessage());
            }
            */

            // Test amaçlı başarılı ödeme simülasyonu
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId("test_transaction_" + System.currentTimeMillis());
            payment.setCompletedAt(LocalDateTime.now());

            return paymentRepository.save(payment);

        } catch (Exception e) {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setErrorMessage(e.getMessage());
            paymentRepository.save(payment);
            throw new RuntimeException("Ödeme işlemi sırasında bir hata oluştu", e);
        }
    }

    @Transactional
    public Payment processCashPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Ödeme bulunamadı"));

        // Nakit ödeme durumunda, ödeme teslimat sırasında yapılacak
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setTransactionId("cash_payment_" + System.currentTimeMillis());

        return paymentRepository.save(payment);
    }

    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Bu sipariş için ödeme bulunamadı"));
    }
    @Transactional
    public Payment updatePaymentStatus(Long paymentId, PaymentStatus status) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Ödeme bulunamadı"));

        payment.setPaymentStatus(status);

        if (status == PaymentStatus.SUCCESS) {
            payment.setCompletedAt(LocalDateTime.now());
        }

        return paymentRepository.save(payment);
    }
}
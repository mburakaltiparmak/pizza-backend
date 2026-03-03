package com.example.pizza.service.payment;

import com.example.pizza.config.logic.IyzicoConfig;
import com.example.pizza.constants.order.OrderStatus;
import com.example.pizza.constants.order.PaymentStatus;
import com.example.pizza.dto.payment.PaymentCardRequest;
import com.example.pizza.dto.payment.PaymentResponse;
import com.example.pizza.entity.order.Order;
import com.example.pizza.entity.order.OrderItem;
import com.example.pizza.entity.logic.Payment;
import com.example.pizza.entity.user.User;
import com.example.pizza.exceptions.common.ResourceNotFoundException;
import com.example.pizza.repository.OrderRepository;
import com.example.pizza.repository.PaymentRepository;
import com.example.pizza.service.logic.EmailService; // [NEW] Email service for payment confirmation
import com.iyzipay.Options;
import com.iyzipay.model.*;
import com.iyzipay.request.CreateCancelRequest;
import com.iyzipay.request.CreatePaymentRequest;
import com.iyzipay.request.CreateRefundRequest;
import com.iyzipay.request.CreateThreedsPaymentRequest;
import com.iyzipay.request.CreateCheckoutFormInitializeRequest;
import com.iyzipay.model.CheckoutFormInitialize;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode; // [NEW] For discount distribution
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IyzicoPaymentServiceImpl implements IyzicoPaymentService {

    private final Options iyzicoOptions;
    private final IyzicoConfig iyzicoConfig;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final EmailService emailService; // [NEW] For sending confirmation email after successful payment

    // =========================================================================
    // PUBLIC API METHODS
    // =========================================================================

    @Override
    @Transactional
    public PaymentResponse processDirectPayment(Long orderId, PaymentCardRequest cardRequest) {
        log.info("Processing direct payment for order: {}", orderId);

        Order order = getOrderOrThrow(orderId);
        Payment payment = getOrCreatePayment(order);

        try {
            // Build Iyzico request
            CreatePaymentRequest request = buildPaymentRequest(order, cardRequest);

            // Call Iyzico API
            com.iyzipay.model.Payment iyzicoPayment = com.iyzipay.model.Payment.create(request, iyzicoOptions);

            // Handle response
            return handlePaymentResult(payment, order, iyzicoPayment);

        } catch (Exception e) {
            log.error("Direct payment failed for order: {}", orderId, e);
            return handlePaymentFailure(payment, "Ödeme işlemi başarısız: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public PaymentResponse initThreeDSPayment(Long orderId, PaymentCardRequest cardRequest, String callbackUrl) {
        log.info("Initializing 3DS payment for order: {}", orderId);

        Order order = getOrderOrThrow(orderId);
        Payment payment = getOrCreatePayment(order);

        try {
            // Build Iyzico 3DS request
            CreatePaymentRequest request = buildPaymentRequest(order, cardRequest);
            request.setCallbackUrl(callbackUrl != null ? callbackUrl : iyzicoConfig.getCallbackUrl());

            // Call Iyzico 3DS Init API
            ThreedsInitialize threedsInitialize = ThreedsInitialize.create(request, iyzicoOptions);

            if ("success".equals(threedsInitialize.getStatus())) {
                // Save 3DS pending state
                payment.setPaymentStatus(PaymentStatus.PENDING_3DS);
                payment.setIyzicoConversationId(orderId.toString());
                payment.setThreeDsHtmlContent(threedsInitialize.getHtmlContent());
                paymentRepository.save(payment);

                log.info("3DS initialized for order: {}", orderId);

                return PaymentResponse.builder()
                        .paymentId(payment.getId())
                        .id(payment.getId())
                        .uuid(payment.getUuid())
                        .status("PENDING_3DS")
                        .paymentStatus(PaymentStatus.PENDING_3DS)
                        .threeDsHtmlContent(threedsInitialize.getHtmlContent())
                        .amount(payment.getAmount())
                        .orderUuid(order.getUuid())
                        .build();
            } else {
                String errorMsg = threedsInitialize.getErrorMessage() != null
                        ? threedsInitialize.getErrorMessage()
                        : "3DS başlatılamadı";
                return handlePaymentFailure(payment, errorMsg);
            }

        } catch (Exception e) {
            log.error("3DS init failed for order: {}", orderId, e);
            return handlePaymentFailure(payment, "3DS başlatma hatası: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public PaymentResponse initCheckoutForm(Long orderId, String callbackUrl) {
        log.info("Initializing Checkout Form for order: {}", orderId);

        Order order = getOrderOrThrow(orderId);
        Payment payment = getOrCreatePayment(order);

        try {
            // Build Checkout Form Request
            CreateCheckoutFormInitializeRequest request = buildCheckoutFormRequest(order, callbackUrl);

            // Call Iyzico API
            CheckoutFormInitialize checkoutFormInitialize = CheckoutFormInitialize.create(request, iyzicoOptions);

            if ("success".equals(checkoutFormInitialize.getStatus())) {
                // Save pending state
                payment.setPaymentStatus(PaymentStatus.PENDING);
                payment.setIyzicoConversationId(orderId.toString());
                payment.setIyzicoToken(checkoutFormInitialize.getToken());
                payment.setThreeDsHtmlContent(checkoutFormInitialize.getCheckoutFormContent());
                paymentRepository.save(payment);

                log.info("Checkout Form initialized for order: {}", orderId);

                return PaymentResponse.builder()
                        .paymentId(payment.getId())
                        .id(payment.getId())
                        .uuid(payment.getUuid())
                        .status("PENDING_CHECKOUT")
                        .paymentStatus(PaymentStatus.PENDING)
                        .threeDsHtmlContent(checkoutFormInitialize.getCheckoutFormContent())
                        .checkoutFormContent(checkoutFormInitialize.getCheckoutFormContent())
                        .token(checkoutFormInitialize.getToken())
                        .paymentPageUrl(checkoutFormInitialize.getPaymentPageUrl())
                        .amount(payment.getAmount())
                        .orderUuid(order.getUuid())
                        .build();
            } else {
                String errorMsg = checkoutFormInitialize.getErrorMessage() != null
                        ? checkoutFormInitialize.getErrorMessage()
                        : "Ödeme formu oluşturulamadı";
                return handlePaymentFailure(payment, errorMsg);
            }

        } catch (Exception e) {
            log.error("Checkout Form init failed for order: {}", orderId, e);
            return handlePaymentFailure(payment, "Ödeme formu hata: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public PaymentResponse handleCheckoutCallback(String token) {
        log.info("Processing Checkout Form callback for token: {}", token);

        try {
            // Retrieve checkout form result
            com.iyzipay.request.RetrieveCheckoutFormRequest request = new com.iyzipay.request.RetrieveCheckoutFormRequest();
            request.setLocale(Locale.TR.getValue());
            request.setConversationId("CHECKOUT_" + token);
            request.setToken(token);

            com.iyzipay.model.CheckoutForm checkoutForm = com.iyzipay.model.CheckoutForm.retrieve(request,
                    iyzicoOptions);

            // Validate retrieval
            if (!"success".equals(checkoutForm.getStatus())) {
                return PaymentResponse.builder()
                        .status("FAILED")
                        .errorMessage("Ödeme sonucu sorgulanamadı: " + checkoutForm.getErrorMessage())
                        .build();
            }

            // Find payment by token from DB
            Payment payment = paymentRepository.findByIyzicoToken(token)
                    .orElseThrow(() -> new ResourceNotFoundException("Bu token ile ödeme bulunamadı: " + token));

            Order order = payment.getOrder();

            // Handle result (same as Direct Payment / 3DS)
            return handlePaymentResult(payment, order, checkoutForm);

        } catch (Exception e) {
            log.error("Checkout callback handling failed", e);
            throw new RuntimeException("Ödeme sonucu işlenemedi: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public PaymentResponse handleThreeDSCallback(String conversationId, String paymentId) {
        log.info("Processing 3DS callback for conversation: {}, payment: {}", conversationId, paymentId);

        try {
            // Create 3DS payment completion request
            CreateThreedsPaymentRequest request = new CreateThreedsPaymentRequest();
            request.setLocale(Locale.TR.getValue());
            request.setConversationId(conversationId);
            request.setPaymentId(paymentId);

            // Call Iyzico to complete 3DS payment
            ThreedsPayment threedsPayment = ThreedsPayment.create(request, iyzicoOptions);

            // Find payment by conversation ID (our order ID)
            Long orderId = Long.parseLong(conversationId);
            Order order = getOrderOrThrow(orderId);
            Payment payment = getPaymentOrThrow(order);

            // Handle 3DS result
            return handle3DSPaymentResult(payment, order, threedsPayment);

        } catch (NumberFormatException e) {
            log.error("Invalid conversation ID format: {}", conversationId);
            throw new IllegalArgumentException("Geçersiz conversation ID: " + conversationId);
        } catch (Exception e) {
            log.error("3DS callback handling failed", e);
            throw new RuntimeException("3DS callback işlemi başarısız: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Ödeme bulunamadı: " + paymentId));
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Bu sipariş için ödeme bulunamadı: " + orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentByUuid(String uuid) {
        return paymentRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Ödeme bulunamadı (UUID): " + uuid));
    }

    @Override
    @Transactional
    public PaymentResponse refundPayment(Long paymentId, BigDecimal amount, String reason) {
        log.info("Processing refund for payment: {}, amount: {}", paymentId, amount);

        Payment payment = getPaymentById(paymentId);

        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
            return PaymentResponse.builder()
                    .paymentId(paymentId)
                    .status("FAILED")
                    .errorMessage("Sadece başarılı ödemeler iade edilebilir")
                    .orderUuid(payment.getOrder().getUuid())
                    .build();
        }

        try {
            CreateRefundRequest request = new CreateRefundRequest();
            request.setLocale(Locale.TR.getValue());
            request.setConversationId(payment.getIyzicoConversationId());
            request.setPaymentTransactionId(payment.getIyzicoPaymentId());
            request.setPrice(amount != null ? amount : payment.getAmount());
            request.setCurrency(Currency.TRY.name());
            request.setIp("85.34.78.112");

            Refund refund = Refund.create(request, iyzicoOptions);

            if ("success".equals(refund.getStatus())) {
                payment.setPaymentStatus(PaymentStatus.REFUNDED);
                payment.setErrorMessage("İade sebebi: " + reason);
                paymentRepository.save(payment);

                log.info("Refund successful for payment: {}", paymentId);

                return PaymentResponse.builder()
                        .paymentId(paymentId)
                        .id(payment.getId())
                        .uuid(payment.getUuid())
                        .status("REFUNDED")
                        .paymentStatus(PaymentStatus.REFUNDED)
                        .amount(payment.getAmount())
                        .orderUuid(payment.getOrder().getUuid())
                        .build();
            } else {
                return PaymentResponse.builder()
                        .paymentId(paymentId)
                        .status("FAILED")
                        .errorMessage(refund.getErrorMessage())
                        .orderUuid(payment.getOrder().getUuid())
                        .build();
            }

        } catch (Exception e) {
            log.error("Refund failed for payment: {}", paymentId, e);
            return PaymentResponse.builder()
                    .paymentId(paymentId)
                    .status("FAILED")
                    .errorMessage("İade işlemi başarısız: " + e.getMessage())
                    .orderUuid(payment.getOrder().getUuid())
                    .build();
        }
    }

    @Override
    @Transactional
    public PaymentResponse cancelPayment(Long paymentId) {
        log.info("Processing cancellation for payment: {}", paymentId);

        Payment payment = getPaymentById(paymentId);

        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
            return PaymentResponse.builder()
                    .paymentId(paymentId)
                    .status("FAILED")
                    .errorMessage("Sadece başarılı ödemeler iptal edilebilir")
                    .orderUuid(payment.getOrder().getUuid())
                    .build();
        }

        try {
            CreateCancelRequest request = new CreateCancelRequest();
            request.setLocale(Locale.TR.getValue());
            request.setConversationId(payment.getIyzicoConversationId());
            request.setPaymentId(payment.getIyzicoPaymentId());
            request.setIp("85.34.78.112");

            Cancel cancel = Cancel.create(request, iyzicoOptions);

            if ("success".equals(cancel.getStatus())) {
                payment.setPaymentStatus(PaymentStatus.CANCELLED);
                paymentRepository.save(payment);

                // Update order status
                Order order = payment.getOrder();
                order.setOrderStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);

                log.info("Cancellation successful for payment: {}", paymentId);

                return PaymentResponse.builder()
                        .paymentId(paymentId)
                        .id(payment.getId())
                        .uuid(payment.getUuid())
                        .status("CANCELLED")
                        .paymentStatus(PaymentStatus.CANCELLED)
                        .orderUuid(order.getUuid())
                        .build();
            } else {
                return PaymentResponse.builder()
                        .paymentId(paymentId)
                        .status("FAILED")
                        .errorMessage(cancel.getErrorMessage())
                        .orderUuid(payment.getOrder().getUuid())
                        .build();
            }

        } catch (Exception e) {
            log.error("Cancellation failed for payment: {}", paymentId, e);
            return PaymentResponse.builder()
                    .paymentId(paymentId)
                    .status("FAILED")
                    .errorMessage("İptal işlemi başarısız: " + e.getMessage())
                    .orderUuid(payment.getOrder().getUuid())
                    .build();
        }
    }

    // =========================================================================
    // HELPER METHODS - REQUEST BUILDING
    // =========================================================================

    private CreatePaymentRequest buildPaymentRequest(Order order, PaymentCardRequest cardRequest) {
        CreatePaymentRequest request = new CreatePaymentRequest();

        // Payment details
        request.setLocale(Locale.TR.getValue());
        request.setConversationId(order.getId().toString());
        request.setPrice(BigDecimal.valueOf(order.getTotalAmount()));
        request.setPaidPrice(BigDecimal.valueOf(order.getTotalAmount()));
        request.setCurrency(Currency.TRY.name());
        request.setInstallment(cardRequest.getInstallment());
        request.setPaymentChannel(PaymentChannel.WEB.name());
        request.setPaymentGroup(PaymentGroup.PRODUCT.name());

        // Card details
        PaymentCard paymentCard = new PaymentCard();
        paymentCard.setCardHolderName(cardRequest.getCardHolderName());
        paymentCard.setCardNumber(cardRequest.getCardNumber());
        paymentCard.setExpireMonth(cardRequest.getExpireMonth());
        paymentCard.setExpireYear(cardRequest.getExpireYear());
        paymentCard.setCvc(cardRequest.getCvc());
        paymentCard.setRegisterCard(cardRequest.getRegisterCard());
        request.setPaymentCard(paymentCard);

        // Buyer info
        Buyer buyer = buildBuyer(order);
        request.setBuyer(buyer);

        // Addresses
        Address shippingAddress = buildAddress(order);
        Address billingAddress = buildAddress(order);
        request.setShippingAddress(shippingAddress);
        request.setBillingAddress(billingAddress);

        // Basket items
        List<BasketItem> basketItems = buildBasketItems(order);
        request.setBasketItems(basketItems);

        return request;
    }

    private CreateCheckoutFormInitializeRequest buildCheckoutFormRequest(Order order, String callbackUrl) {
        CreateCheckoutFormInitializeRequest request = new CreateCheckoutFormInitializeRequest();

        request.setLocale(Locale.TR.getValue());
        request.setConversationId(order.getId().toString());
        request.setPrice(BigDecimal.valueOf(order.getTotalAmount()));
        request.setPaidPrice(BigDecimal.valueOf(order.getTotalAmount()));
        request.setCurrency(Currency.TRY.name());
        request.setBasketId("BASKET_" + order.getId());
        request.setPaymentGroup(PaymentGroup.PRODUCT.name());

        // This is where Iyzico will redirect after payment
        request.setCallbackUrl(callbackUrl != null ? callbackUrl : iyzicoConfig.getCallbackUrl());

        // Buyer info
        Buyer buyer = buildBuyer(order);
        request.setBuyer(buyer);

        // Addresses
        Address shippingAddress = buildAddress(order);
        Address billingAddress = buildAddress(order);
        request.setShippingAddress(shippingAddress);
        request.setBillingAddress(billingAddress);

        // Basket items
        List<BasketItem> basketItems = buildBasketItems(order);
        request.setBasketItems(basketItems);

        return request;
    }

    private Buyer buildBuyer(Order order) {
        Buyer buyer = new Buyer();
        User user = order.getUser();

        if (user != null) {
            // Registered user
            buyer.setId(user.getId().toString());
            buyer.setName(user.getName() != null ? user.getName() : "Customer");
            buyer.setSurname(user.getSurname() != null ? user.getSurname() : "User");
            buyer.setEmail(user.getEmail());
            buyer.setGsmNumber(user.getPhoneNumber() != null ? user.getPhoneNumber() : "+905350000000");
            buyer.setIdentityNumber("11111111111"); // Test TC kimlik no
        } else {
            // Guest user
            buyer.setId("GUEST_" + order.getId());
            String guestName = order.getDeliveryAddress() != null
                    && order.getDeliveryAddress().getRecipientName() != null
                            ? order.getDeliveryAddress().getRecipientName()
                            : "Guest";
            buyer.setName(guestName.contains(" ") ? guestName.split(" ")[0] : guestName);
            buyer.setSurname(guestName.contains(" ") ? guestName.split(" ")[1] : "User");
            buyer.setEmail(order.getGuestEmail() != null ? order.getGuestEmail() : "guest@teknolojik-yemekler.com");
            buyer.setGsmNumber(order.getDeliveryAddress() != null && order.getDeliveryAddress().getPhoneNumber() != null
                    ? order.getDeliveryAddress().getPhoneNumber()
                    : "+905350000000");
            buyer.setIdentityNumber("11111111111");
        }

        // Common fields
        buyer.setRegistrationAddress(
                order.getDeliveryAddress() != null && order.getDeliveryAddress().getFullAddress() != null
                        ? order.getDeliveryAddress().getFullAddress()
                        : "Default Address");
        buyer.setIp("85.34.78.112");
        buyer.setCity(order.getDeliveryAddress() != null && order.getDeliveryAddress().getCity() != null
                ? order.getDeliveryAddress().getCity()
                : "Istanbul");
        buyer.setCountry("Turkey");
        buyer.setZipCode(order.getDeliveryAddress() != null && order.getDeliveryAddress().getPostalCode() != null
                ? order.getDeliveryAddress().getPostalCode()
                : "34000");

        return buyer;
    }

    private Address buildAddress(Order order) {
        Address address = new Address();

        if (order.getDeliveryAddress() != null) {
            String contactName = order.getDeliveryAddress().getRecipientName() != null
                    ? order.getDeliveryAddress().getRecipientName()
                    : "Customer";
            address.setContactName(contactName);
            address.setCity(order.getDeliveryAddress().getCity() != null
                    ? order.getDeliveryAddress().getCity()
                    : "Istanbul");
            address.setCountry("Turkey");
            address.setAddress(order.getDeliveryAddress().getFullAddress() != null
                    ? order.getDeliveryAddress().getFullAddress()
                    : "Default Address");
            address.setZipCode(order.getDeliveryAddress().getPostalCode() != null
                    ? order.getDeliveryAddress().getPostalCode()
                    : "34000");
        } else {
            address.setContactName("Customer");
            address.setCity("Istanbul");
            address.setCountry("Turkey");
            address.setAddress("Default Address");
            address.setZipCode("34000");
        }

        return address;
    }

    // [FIX] Promo code indirimi basket item'lara orantılı dağıtılıyor
    // Iyzico kuralı: price == sum(basketItems.price) olmalı
    private List<BasketItem> buildBasketItems(Order order) {
        List<BasketItem> basketItems = new ArrayList<>();

        if (order.getItems() != null && !order.getItems().isEmpty()) {
            for (OrderItem item : order.getItems()) {
                BasketItem basketItem = new BasketItem();
                basketItem.setId(
                        item.getProduct() != null ? item.getProduct().getId().toString() : "ITEM_" + item.getId());
                basketItem.setName(item.getProduct() != null ? item.getProduct().getName() : "Product");
                basketItem.setCategory1(item.getProduct() != null && item.getProduct().getCategory() != null
                        ? item.getProduct().getCategory().getName()
                        : "Food");
                basketItem.setCategory2("Pizza");
                basketItem.setItemType(BasketItemType.PHYSICAL.name());

                BigDecimal itemTotal = BigDecimal.valueOf(item.getPrice())
                        .multiply(BigDecimal.valueOf(item.getQuantity()));
                basketItem.setPrice(itemTotal);

                basketItems.add(basketItem);
            }

            // ================================================================
            // Promo code indirimi varsa, item'lara orantılı olarak dağıt
            // order.totalAmount zaten indirimli, ama item fiyatları orijinal
            // ================================================================
            if (order.getDiscountAmount() > 0 && !basketItems.isEmpty()) {
                BigDecimal discount = BigDecimal.valueOf(order.getDiscountAmount());

                BigDecimal itemsTotal = basketItems.stream()
                        .map(BasketItem::getPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (itemsTotal.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal distributedDiscount = BigDecimal.ZERO;

                    for (int i = 0; i < basketItems.size(); i++) {
                        BasketItem bi = basketItems.get(i);
                        BigDecimal itemDiscount;

                        if (i == basketItems.size() - 1) {
                            // Son item'a kalan indirimi ver (yuvarlama farkını önler)
                            itemDiscount = discount.subtract(distributedDiscount);
                        } else {
                            // Orantılı indirim: (itemPrice / itemsTotal) * discount
                            itemDiscount = bi.getPrice()
                                    .divide(itemsTotal, 10, RoundingMode.HALF_UP)
                                    .multiply(discount)
                                    .setScale(2, RoundingMode.HALF_UP);
                            distributedDiscount = distributedDiscount.add(itemDiscount);
                        }

                        bi.setPrice(bi.getPrice().subtract(itemDiscount));
                    }

                    log.debug("Discount {} distributed across {} basket items. Items total before: {}, after: {}",
                            discount, basketItems.size(), itemsTotal, itemsTotal.subtract(discount));
                }
            }
        } else {
            // Fallback - single item with total amount (zaten indirimli)
            BasketItem basketItem = new BasketItem();
            basketItem.setId("ORDER_" + order.getId());
            basketItem.setName("Sipariş #" + order.getId());
            basketItem.setCategory1("Food");
            basketItem.setCategory2("Pizza");
            basketItem.setItemType(BasketItemType.PHYSICAL.name());
            basketItem.setPrice(BigDecimal.valueOf(order.getTotalAmount()));
            basketItems.add(basketItem);
        }

        return basketItems;
    }

    // =========================================================================
    // HELPER METHODS - RESPONSE HANDLING
    // =========================================================================

    private PaymentResponse handlePaymentResult(Payment payment, Order order,
            com.iyzipay.model.CheckoutForm checkoutForm) {
        log.info("Handling payment result for Order ID: {}, UUID: {}", order.getId(), order.getUuid());
        if ("success".equals(checkoutForm.getStatus())) {
            // Payment successful - update entities
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            payment.setIyzicoPaymentId(checkoutForm.getPaymentId());
            payment.setIyzicoConversationId(checkoutForm.getConversationId());
            payment.setAuthCode(checkoutForm.getAuthCode());
            payment.setCardAssociation(checkoutForm.getCardAssociation());
            payment.setCardFamily(checkoutForm.getCardFamily());
            payment.setCardBinNumber(checkoutForm.getBinNumber());
            payment.setCardLastFour(checkoutForm.getLastFourDigits());
            payment.setFraudStatus(checkoutForm.getFraudStatus());
            payment.setInstallment(checkoutForm.getInstallment());
            payment.setTransactionId(checkoutForm.getPaymentId());

            payment.setCompletedAt(LocalDateTime.now());

            // Update order status
            order.setOrderStatus(OrderStatus.CONFIRMED);

            // Save
            paymentRepository.save(payment);
            orderRepository.save(order);

            log.info("Checkout payment successful for order: {}, paymentId: {}",
                    order.getId(), checkoutForm.getPaymentId());

            // [NEW] Send confirmation email now that payment is confirmed
            sendPaymentConfirmationEmail(order);

            BigDecimal paidAmount = checkoutForm.getPaidPrice() != null
                    ? checkoutForm.getPaidPrice()
                    : payment.getAmount();

            return PaymentResponse.builder()
                    .paymentId(payment.getId())
                    .id(payment.getId())
                    .uuid(payment.getUuid())
                    .status("SUCCESS")
                    .paymentStatus(PaymentStatus.SUCCESS)
                    .iyzicoPaymentId(checkoutForm.getPaymentId())
                    .iyzicoConversationId(checkoutForm.getConversationId())
                    .transactionId(checkoutForm.getPaymentId())
                    .authCode(checkoutForm.getAuthCode())
                    .amount(payment.getAmount())
                    .paidAmount(paidAmount)
                    .currency("TRY")
                    .cardAssociation(checkoutForm.getCardAssociation())
                    .cardFamily(checkoutForm.getCardFamily())
                    .cardLastFour(checkoutForm.getLastFourDigits())
                    .installment(checkoutForm.getInstallment())
                    .completedAt(payment.getCompletedAt())
                    .orderUuid(order.getUuid())
                    .build();
        } else {
            return handlePaymentFailure(payment, checkoutForm.getErrorMessage());
        }
    }

    private PaymentResponse handlePaymentResult(Payment payment, Order order,
            com.iyzipay.model.Payment iyzicoPayment) {
        if ("success".equals(iyzicoPayment.getStatus())) {
            // Payment successful - update entities
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            payment.setIyzicoPaymentId(iyzicoPayment.getPaymentId());
            payment.setIyzicoConversationId(iyzicoPayment.getConversationId());
            payment.setAuthCode(iyzicoPayment.getAuthCode());
            payment.setCardAssociation(iyzicoPayment.getCardAssociation());
            payment.setCardFamily(iyzicoPayment.getCardFamily());
            payment.setCardBinNumber(iyzicoPayment.getBinNumber());
            payment.setCardLastFour(iyzicoPayment.getLastFourDigits());
            payment.setFraudStatus(iyzicoPayment.getFraudStatus());
            payment.setInstallment(iyzicoPayment.getInstallment());
            payment.setTransactionId(iyzicoPayment.getPaymentId());

            // Commission details
            if (iyzicoPayment.getMerchantCommissionRate() != null) {
                payment.setMerchantCommissionRate(iyzicoPayment.getMerchantCommissionRate());
            }
            if (iyzicoPayment.getMerchantCommissionRateAmount() != null) {
                payment.setMerchantCommissionAmount(iyzicoPayment.getMerchantCommissionRateAmount());
            }
            if (iyzicoPayment.getIyziCommissionRateAmount() != null) {
                payment.setIyzicoCommissionRate(iyzicoPayment.getIyziCommissionRateAmount());
            }
            if (iyzicoPayment.getIyziCommissionFee() != null) {
                payment.setIyzicoCommissionFee(iyzicoPayment.getIyziCommissionFee());
            }

            // Calculate merchant payout from item transactions
            BigDecimal totalPayout = BigDecimal.ZERO;
            if (iyzicoPayment.getPaymentItems() != null) {
                for (PaymentItem item : iyzicoPayment.getPaymentItems()) {
                    if (item.getMerchantPayoutAmount() != null) {
                        totalPayout = totalPayout.add(item.getMerchantPayoutAmount());
                    }
                }
            }
            payment.setMerchantPayoutAmount(totalPayout);
            payment.setCompletedAt(LocalDateTime.now());

            // Update order status
            order.setOrderStatus(OrderStatus.CONFIRMED);

            // Save
            paymentRepository.save(payment);
            orderRepository.save(order);

            log.info("Payment successful for order: {}, paymentId: {}",
                    order.getId(), iyzicoPayment.getPaymentId());

            // [NEW] Send confirmation email now that payment is confirmed
            sendPaymentConfirmationEmail(order);

            return PaymentResponse.builder()
                    .paymentId(payment.getId())
                    .id(payment.getId())
                    .uuid(payment.getUuid())
                    .status("SUCCESS")
                    .paymentStatus(PaymentStatus.SUCCESS)
                    .iyzicoPaymentId(iyzicoPayment.getPaymentId())
                    .iyzicoConversationId(iyzicoPayment.getConversationId())
                    .transactionId(iyzicoPayment.getPaymentId())
                    .authCode(iyzicoPayment.getAuthCode())
                    .amount(payment.getAmount())
                    .paidAmount(
                            iyzicoPayment.getPaidPrice() != null ? iyzicoPayment.getPaidPrice() : payment.getAmount())
                    .merchantPayoutAmount(totalPayout)
                    .currency("TRY")
                    .cardAssociation(iyzicoPayment.getCardAssociation())
                    .cardFamily(iyzicoPayment.getCardFamily())
                    .cardLastFour(iyzicoPayment.getLastFourDigits())
                    .installment(iyzicoPayment.getInstallment())
                    .completedAt(payment.getCompletedAt())
                    .orderUuid(order.getUuid())
                    .build();
        } else {
            return handlePaymentFailure(payment, iyzicoPayment.getErrorMessage());
        }
    }

    private PaymentResponse handle3DSPaymentResult(Payment payment, Order order,
            ThreedsPayment threedsPayment) {
        if ("success".equals(threedsPayment.getStatus())) {
            // 3DS payment successful
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            payment.setIyzicoPaymentId(threedsPayment.getPaymentId());
            payment.setAuthCode(threedsPayment.getAuthCode());
            payment.setCardAssociation(threedsPayment.getCardAssociation());
            payment.setCardFamily(threedsPayment.getCardFamily());
            payment.setCardBinNumber(threedsPayment.getBinNumber());
            payment.setCardLastFour(threedsPayment.getLastFourDigits());
            payment.setFraudStatus(threedsPayment.getFraudStatus());
            payment.setTransactionId(threedsPayment.getPaymentId());
            payment.setCompletedAt(LocalDateTime.now());
            payment.setThreeDsHtmlContent(null); // Clear HTML content

            // Update order status
            order.setOrderStatus(OrderStatus.CONFIRMED);

            paymentRepository.save(payment);
            orderRepository.save(order);

            log.info("3DS payment successful for order: {}", order.getId());

            // [NEW] Send confirmation email now that payment is confirmed
            sendPaymentConfirmationEmail(order);

            return PaymentResponse.builder()
                    .paymentId(payment.getId())
                    .id(payment.getId())
                    .uuid(payment.getUuid())
                    .status("SUCCESS")
                    .paymentStatus(PaymentStatus.SUCCESS)
                    .iyzicoPaymentId(threedsPayment.getPaymentId())
                    .transactionId(threedsPayment.getPaymentId())
                    .authCode(threedsPayment.getAuthCode())
                    .cardAssociation(threedsPayment.getCardAssociation())
                    .cardFamily(threedsPayment.getCardFamily())
                    .cardLastFour(threedsPayment.getLastFourDigits())
                    .amount(payment.getAmount())
                    .completedAt(payment.getCompletedAt())
                    .orderUuid(order.getUuid())
                    .build();
        } else {
            return handlePaymentFailure(payment, threedsPayment.getErrorMessage());
        }
    }

    private PaymentResponse handlePaymentFailure(Payment payment, String errorMessage) {
        payment.setPaymentStatus(PaymentStatus.FAILED);
        payment.setErrorMessage(errorMessage);
        paymentRepository.save(payment);

        log.warn("Payment failed for order: {}, error: {}",
                payment.getOrder().getId(), errorMessage);

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .id(payment.getId())
                .uuid(payment.getUuid())
                .status("FAILED")
                .paymentStatus(PaymentStatus.FAILED)
                .errorMessage(errorMessage)
                .amount(payment.getAmount())
                .orderUuid(payment.getOrder().getUuid())
                .build();
    }

    // =========================================================================
    // HELPER METHODS - EMAIL
    // =========================================================================

    /**
     * [NEW] Send order confirmation email after successful payment.
     * Used for ONLINE_CREDIT_CARD / 3DS orders where email is deferred
     * until payment completes. Email failure should never break the payment flow.
     */
    private void sendPaymentConfirmationEmail(Order order) {
        try {
            String email = order.getOrderEmail();
            if (email != null) {
                emailService.sendOrderConfirmationEmail(order, order.getUser());
                log.info("Payment confirmation email sent to: {} for order: {}", email, order.getId());
            }
        } catch (Exception e) {
            log.error("Failed to send payment confirmation email for order {}: {}",
                    order.getId(), e.getMessage());
        }
    }

    // =========================================================================
    // HELPER METHODS - VALIDATION & RETRIEVAL
    // =========================================================================

    private Order getOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş bulunamadı: " + orderId));
    }

    private Payment getPaymentOrThrow(Order order) {
        if (order.getPayment() == null) {
            throw new ResourceNotFoundException("Bu sipariş için ödeme bulunamadı: " + order.getId());
        }
        return order.getPayment();
    }

    private Payment getOrCreatePayment(Order order) {
        if (order.getPayment() != null) {
            return order.getPayment();
        }
        throw new ResourceNotFoundException("Bu sipariş için ödeme kaydı bulunamadı: " + order.getId());
    }

    // Helper to reuse logic safely
    private Payment getOrderedPayment(Order order) {
        if (order.getPayment() != null)
            return order.getPayment();
        return paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Ödeme bulunamadı for order: " + order.getId()));
    }
}
package com.example.pizza.service;

import com.example.pizza.constants.promo.DiscountType;
import com.example.pizza.entity.order.Order;
import com.example.pizza.entity.order.PromoCode;
import com.example.pizza.entity.order.PromoCodeUsage;
import com.example.pizza.entity.user.User;
import com.example.pizza.exceptions.order.InvalidPromoCodeException;
import com.example.pizza.repository.PromoCodeRepository;
import com.example.pizza.repository.PromoCodeUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;
    private final PromoCodeUsageRepository promoCodeUsageRepository;

    @Transactional
    public PromoCode createPromoCode(PromoCode promoCode) {
        if (promoCodeRepository.findByCode(promoCode.getCode().toUpperCase()).isPresent()) {
            throw new InvalidPromoCodeException("Bu promosyon kodu zaten mevcut.");
        }
        return promoCodeRepository.save(promoCode);
    }

    public List<PromoCode> getAllPromoCodes() {
        return promoCodeRepository.findAll();
    }

    public PromoCode getPromoCodeById(Long id) {
        return promoCodeRepository.findById(id)
                .orElseThrow(() -> new InvalidPromoCodeException("Promosyon kodu bulunamadı: ID " + id));
    }

    @Transactional
    public PromoCode updatePromoCode(Long id, PromoCode updatedPromoCode) {
        PromoCode existingPromoCode = getPromoCodeById(id);

        // Update fields
        // Code cannot be changed easily if unique constraint logic is complex, but
        // let's allow basic updates
        if (updatedPromoCode.getCode() != null
                && !updatedPromoCode.getCode().equalsIgnoreCase(existingPromoCode.getCode())) {
            if (promoCodeRepository.findByCode(updatedPromoCode.getCode().toUpperCase()).isPresent()) {
                throw new InvalidPromoCodeException("Bu promosyon kodu zaten mevcut.");
            }
            existingPromoCode.setCode(updatedPromoCode.getCode().toUpperCase());
        }

        existingPromoCode.setDiscountType(updatedPromoCode.getDiscountType());
        existingPromoCode.setDiscountValue(updatedPromoCode.getDiscountValue());
        existingPromoCode.setMinOrderAmount(updatedPromoCode.getMinOrderAmount());
        existingPromoCode.setMaxDiscountAmount(updatedPromoCode.getMaxDiscountAmount());
        existingPromoCode.setValidFrom(updatedPromoCode.getValidFrom());
        existingPromoCode.setValidUntil(updatedPromoCode.getValidUntil());
        existingPromoCode.setUsageLimit(updatedPromoCode.getUsageLimit());
        existingPromoCode.setPerUserLimit(updatedPromoCode.getPerUserLimit());
        existingPromoCode.setActive(updatedPromoCode.isActive());

        return promoCodeRepository.save(existingPromoCode);
    }

    @Transactional
    public void deletePromoCode(Long id) {
        PromoCode promoCode = getPromoCodeById(id);
        // Soft delete or hard delete? Let's check constraints.
        // If usages exist, hard delete will fail due to FK.
        // Better to allow soft delete or check usages.
        // For simplicity now: Hard delete. If fails, user must remove usages or disable
        // it.
        // Ideally should check usages.

        long usageCount = promoCodeUsageRepository.countByPromoCode(promoCode);
        if (usageCount > 0) {
            throw new InvalidPromoCodeException("Bu promosyon kodu kullanılmıştır, silinemez. Pasife alabilirsiniz.");
        }

        promoCodeRepository.delete(promoCode);
    }

    public BigDecimal validatePromoCode(String code, User user, BigDecimal currentTotalAmount) {
        PromoCode promoCode = promoCodeRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new InvalidPromoCodeException("Geçersiz promosyon kodu."));

        if (!promoCode.isActive()) {
            throw new InvalidPromoCodeException("Bu promosyon kodu aktif değil.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (promoCode.getValidFrom() != null && now.isBefore(promoCode.getValidFrom())) {
            throw new InvalidPromoCodeException("Bu promosyon kodu henüz başlamadı.");
        }

        if (promoCode.getValidUntil() != null && now.isAfter(promoCode.getValidUntil())) {
            throw new InvalidPromoCodeException("Bu promosyon kodunun süresi dolmuş.");
        }

        if (promoCode.getMinOrderAmount() != null && currentTotalAmount.compareTo(promoCode.getMinOrderAmount()) < 0) {
            throw new InvalidPromoCodeException("Sepet tutarı bu promosyon kodu için yetersiz.");
        }

        if (promoCode.getUsageLimit() != null) {
            long totalUsage = promoCodeUsageRepository.countByPromoCode(promoCode);
            if (totalUsage >= promoCode.getUsageLimit()) {
                throw new InvalidPromoCodeException("Bu promosyon kodunun kullanım limiti dolmuş.");
            }
        }

        if (user != null && promoCode.getPerUserLimit() != null) {
            long userUsage = promoCodeUsageRepository.countByPromoCodeAndUser(promoCode, user);
            if (userUsage >= promoCode.getPerUserLimit()) {
                throw new InvalidPromoCodeException("Bu promosyon kodunu kullanma hakkınız dolmuş.");
            }
        }

        return calculateDiscount(promoCode, currentTotalAmount);
    }

    private BigDecimal calculateDiscount(PromoCode promoCode, BigDecimal totalAmount) {
        BigDecimal discount = BigDecimal.ZERO;

        if (promoCode.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            discount = promoCode.getDiscountValue();
        } else if (promoCode.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = totalAmount
                    .multiply(promoCode.getDiscountValue().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        }

        if (promoCode.getMaxDiscountAmount() != null && discount.compareTo(promoCode.getMaxDiscountAmount()) > 0) {
            discount = promoCode.getMaxDiscountAmount();
        }

        // Discount cannot be greater than total amount
        if (discount.compareTo(totalAmount) > 0) {
            discount = totalAmount;
        }

        return discount;
    }

    @Transactional
    public void applyPromoCode(Order order, String code) {
        // Validation calls validatePromoCode which returns discount but here we just
        // need to ensure valid and apply
        // Note: applyPromoCode assumes order total is already set or calculated.

        // We re-validate briefly or reuse validate logic.
        // Since we need to record usage, we must pass User object if order has one.

        if (code == null || code.isEmpty())
            return;

        BigDecimal total = BigDecimal.valueOf(order.getTotalAmount());
        User user = order.getUser(); // Can be null for guest orders if logic allows guest usage?
        // Our validation logic checks perUserLimit ONLY IF user != null.
        // If guest orders shouldn't use promo codes with user limits, that logic holds.

        BigDecimal discount = validatePromoCode(code, user, total);

        order.setPromoCode(promoCodeRepository.findByCode(code.toUpperCase()).orElseThrow());
        order.setDiscountAmount(discount.doubleValue());

        // Update Total Amount (Final Amount to Pay)
        order.setTotalAmount(total.subtract(discount).doubleValue());

    }

    public void recordUsage(Order order) {
        if (order.getPromoCode() == null) {
            return;
        }
        PromoCodeUsage usage = new PromoCodeUsage();
        usage.setPromoCode(order.getPromoCode());
        usage.setOrder(order);
        usage.setUser(order.getUser());
        promoCodeUsageRepository.save(usage);
    }
}

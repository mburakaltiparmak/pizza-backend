package com.example.pizza.service;

import com.example.pizza.constants.promo.DiscountType;
import com.example.pizza.entity.order.Order;
import com.example.pizza.entity.order.PromoCode;
import com.example.pizza.entity.user.User;
import com.example.pizza.exceptions.order.InvalidPromoCodeException;
import com.example.pizza.repository.PromoCodeRepository;
import com.example.pizza.repository.PromoCodeUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PromoCodeServiceTest {

    @Mock
    private PromoCodeRepository promoCodeRepository;

    @Mock
    private PromoCodeUsageRepository promoCodeUsageRepository;

    @InjectMocks
    private PromoCodeService promoCodeService;

    private PromoCode promoCode;
    private User user;

    @BeforeEach
    void setUp() {
        promoCode = new PromoCode();
        promoCode.setId(1L);
        promoCode.setCode("SAVE10");
        promoCode.setDiscountType(DiscountType.PERCENTAGE);
        promoCode.setDiscountValue(BigDecimal.valueOf(10)); // 10%
        promoCode.setActive(true);
        promoCode.setValidFrom(LocalDateTime.now().minusDays(1));
        promoCode.setValidUntil(LocalDateTime.now().plusDays(1));

        user = new User();
        user.setId(1L);
    }

    @Test
    void validatePromoCode_ShouldReturnDiscount_WhenValid_Percentage() {
        promoCode.setDiscountType(DiscountType.PERCENTAGE);
        promoCode.setDiscountValue(BigDecimal.valueOf(20)); // 20%

        when(promoCodeRepository.findByCode("SAVE20")).thenReturn(Optional.of(promoCode));

        BigDecimal totalAmount = BigDecimal.valueOf(1000.00);
        BigDecimal discount = promoCodeService.validatePromoCode("SAVE20", user, totalAmount);

        // 20% of 1000 is 200
        assertEquals(0, BigDecimal.valueOf(200.00).compareTo(discount),
                "Percentage discount calculation failed. Expected 200 (20%), got " + discount);
    }

    @Test
    void validatePromoCode_ShouldReturnDiscount_WhenValid_Fixed() {
        PromoCode fixedPromo = new PromoCode();
        fixedPromo.setCode("FIXED50");
        fixedPromo.setActive(true);
        fixedPromo.setDiscountType(DiscountType.FIXED_AMOUNT);
        fixedPromo.setDiscountValue(BigDecimal.valueOf(50));

        when(promoCodeRepository.findByCode("FIXED50")).thenReturn(Optional.of(fixedPromo));

        BigDecimal totalAmount = BigDecimal.valueOf(1000.00);
        BigDecimal discount = promoCodeService.validatePromoCode("FIXED50", user, totalAmount);

        // Fixed 50
        assertEquals(0, BigDecimal.valueOf(50.00).compareTo(discount));
    }

    @Test
    void validatePromoCode_ShouldThrowException_WhenCodeNotFound() {
        when(promoCodeRepository.findByCode("INVALID")).thenReturn(Optional.empty());

        assertThrows(InvalidPromoCodeException.class,
                () -> promoCodeService.validatePromoCode("INVALID", user, BigDecimal.valueOf(100)));
    }

    @Test
    void validatePromoCode_ShouldThrowException_WhenExpired() {
        promoCode.setValidUntil(LocalDateTime.now().minusDays(1));
        when(promoCodeRepository.findByCode("SAVE10")).thenReturn(Optional.of(promoCode));

        assertThrows(InvalidPromoCodeException.class,
                () -> promoCodeService.validatePromoCode("SAVE10", user, BigDecimal.valueOf(100)));
    }

    @Test
    void validatePromoCode_ShouldThrowException_WhenLimitReached() {
        promoCode.setUsageLimit(5);
        when(promoCodeRepository.findByCode("SAVE10")).thenReturn(Optional.of(promoCode));
        when(promoCodeUsageRepository.countByPromoCode(promoCode)).thenReturn(5L);

        assertThrows(InvalidPromoCodeException.class,
                () -> promoCodeService.validatePromoCode("SAVE10", user, BigDecimal.valueOf(100)));
    }

    @Test
    void applyPromoCode_ShouldUpdateOrder() {
        Order order = new Order();
        order.setTotalAmount(200.00); // 200 TL
        order.setUser(user);

        when(promoCodeRepository.findByCode("SAVE10")).thenReturn(Optional.of(promoCode));

        // validate checks
        // (mocks for validatePromoCode logic are implicit as we use real method but
        // mocked repo calls)

        promoCodeService.applyPromoCode(order, "SAVE10");

        // 10% of 200 = 20. Total should be 180.
        // Discount 20.
        assertEquals(20.0, order.getDiscountAmount());
        assertEquals(180.0, order.getTotalAmount());

    }

    @Test
    void recordUsage_ShouldSaveUsage_WhenPromoCodeExists() {
        Order order = new Order();
        order.setPromoCode(promoCode);
        order.setUser(user);

        promoCodeService.recordUsage(order);

        verify(promoCodeUsageRepository, times(1)).save(any());
    }
}

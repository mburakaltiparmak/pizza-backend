package com.example.pizza.controller;

import com.example.pizza.entity.order.PromoCode;
import com.example.pizza.service.PromoCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/promo-codes")
@RequiredArgsConstructor
public class PromoCodeController {

    private final PromoCodeService promoCodeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromoCode> createPromoCode(@RequestBody PromoCode promoCode) {
        return ResponseEntity.ok(promoCodeService.createPromoCode(promoCode));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PromoCode>> getAllPromoCodes() {
        return ResponseEntity.ok(promoCodeService.getAllPromoCodes());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromoCode> getPromoCodeById(@PathVariable Long id) {
        return ResponseEntity.ok(promoCodeService.getPromoCodeById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromoCode> updatePromoCode(@PathVariable Long id, @RequestBody PromoCode promoCode) {
        return ResponseEntity.ok(promoCodeService.updatePromoCode(id, promoCode));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePromoCode(@PathVariable Long id) {
        promoCodeService.deletePromoCode(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validatePromoCode(
            @RequestParam String code,
            @RequestParam BigDecimal amount) { // Simplified for now, userId handled in OrderService for final apply

        // We can't easily check per-user limit here without authenticated user.
        // If we want to check, we should resolve user from security context.
        // For now, checks basic validity and amount.

        BigDecimal discount = promoCodeService.validatePromoCode(code, null, amount);

        return ResponseEntity.ok(Map.of(
                "valid", true,
                "discountAmount", discount,
                "code", code));
    }
}

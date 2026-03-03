package com.example.pizza.repository;

import com.example.pizza.entity.order.PromoCode;
import com.example.pizza.entity.order.PromoCodeUsage;
import com.example.pizza.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromoCodeUsageRepository extends JpaRepository<PromoCodeUsage, Long> {
    long countByPromoCode(PromoCode promoCode);

    long countByPromoCodeAndUser(PromoCode promoCode, User user);
}

package com.example.pizza.repository;

import com.example.pizza.entity.logic.Payment;
import com.example.pizza.constants.order.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByPaymentStatus(PaymentStatus status);
    Optional<Payment> findByOrderId(Long orderId);
}
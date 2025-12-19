package com.example.pizza.entity.order;

import com.example.pizza.constants.order.OrderStatus;
import com.example.pizza.constants.user.Role;
import com.example.pizza.entity.logic.Payment;
import com.example.pizza.entity.user.User;
import com.example.pizza.entity.user.UserAddress;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(schema = "pizza", name = "orders")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    @JsonBackReference(value = "user-order")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_role", nullable = false)
    private Role orderRole;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "fullAddress", column = @Column(name = "delivery_address")),
            @AttributeOverride(name = "city", column = @Column(name = "delivery_city")),
            @AttributeOverride(name = "district", column = @Column(name = "delivery_district")),
            @AttributeOverride(name = "postalCode", column = @Column(name = "delivery_postal_code")),
            @AttributeOverride(name = "phoneNumber", column = @Column(name = "delivery_phone")),
            @AttributeOverride(name = "recipientName", column = @Column(name = "delivery_recipient_name")),
            @AttributeOverride(name = "addressTitle", column = @Column(name = "delivery_address_title", nullable = true)),
            @AttributeOverride(name = "id", column = @Column(name = "delivery_address_id", nullable = true)),
            @AttributeOverride(name = "isDefault", column = @Column(name = "delivery_is_default", nullable = true)),
            @AttributeOverride(name = "createdAt", column = @Column(name = "delivery_created_at", nullable = true)),
            @AttributeOverride(name = "updatedAt", column = @Column(name = "delivery_updated_at", nullable = true))
    })
    private UserAddress deliveryAddress;

    @Column(name = "guest_email")
    private String guestEmail;

    @Column(name = "order_date", nullable = false)
    private LocalDateTime orderDate = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus;

    @Column(name = "total_amount", nullable = false)
    private double totalAmount;

    @Column(name = "notes")
    private String notes;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference(value = "order-items")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference(value = "order-payment")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Payment payment;

    @PrePersist
    @PreUpdate
    private void prepareAndValidateOrder() {
        // 1. Role ve guestEmail ayarla (setRoleAndEmail metodunun içeriği)
        if (this.user != null && this.user.getId() != null) {
            this.orderRole = Role.CUSTOMER;
            this.guestEmail = null;
        } else {
            this.orderRole = Role.GUEST;
            if (this.deliveryAddress != null && this.deliveryAddress.getEmail() != null) {
                this.guestEmail = this.deliveryAddress.getEmail();
            } else {
                this.guestEmail = null;
            }
        }

        // 2. Validasyonları yap (validateOrder metodunun içeriği)
        if (this.deliveryAddress == null) {
            throw new IllegalStateException("Teslimat adresi gereklidir");
        }

        if (this.orderRole == Role.GUEST) {
            if (this.deliveryAddress.getEmail() == null || this.deliveryAddress.getEmail().trim().isEmpty()) {
                throw new IllegalStateException("Misafir siparişi için email gereklidir");
            }
            if (this.deliveryAddress.getRecipientName() == null || this.deliveryAddress.getRecipientName().trim().isEmpty()) {
                throw new IllegalStateException("Misafir siparişi için alıcı adı gereklidir");
            }
        }
    }

    public String getOrderEmail() {
        return user != null ? user.getEmail() : guestEmail;
    }

    public String getOrderName() {
        if (user != null) {
            return user.getName() + " " + user.getSurname();
        }
        return deliveryAddress != null ? deliveryAddress.getRecipientName() : null;
    }

    public boolean isGuestOrder() {
        return orderRole == Role.GUEST;
    }
}
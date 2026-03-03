package com.example.pizza.dto.socket;

import com.example.pizza.constants.order.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSocketDTO {

    private Long id;
    private String orderNumber;
    private OrderStatus orderStatus;
    private Double totalAmount;
    private String customerName;
    private String customerEmail;
    private LocalDateTime orderDate;
    private Integer itemCount;
    private List<OrderItemSocketDTO> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemSocketDTO {
        private Long productId;
        private String productName;
        private Integer quantity;
        private Double price;
    }
}

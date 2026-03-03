package com.example.pizza.service.socket;

import com.corundumstudio.socketio.SocketIOServer;
import com.example.pizza.dto.socket.OrderSocketDTO;
import com.example.pizza.entity.order.Order;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocketIOService {

    private final SocketIOServer server;
    private final com.example.pizza.logic.mapper.OrderMapper orderMapper;

    @PostConstruct
    public void startServer() {
        try {
            server.start();

            // Connection Listeners
            server.addConnectListener(client -> {
                log.info("SocketIO Client Connected: ID={}, Remote={}", client.getSessionId(),
                        client.getRemoteAddress());
            });

            server.addDisconnectListener(client -> {
                log.info("SocketIO Client Disconnected: ID={}", client.getSessionId());
            });

            // Room Join Listener - CRITICAL for admin dashboard
            server.addEventListener("join", String.class, (client, room, ackRequest) -> {
                client.joinRoom(room);
                log.info("Client {} joined room: {}", client.getSessionId(), room);

                if (ackRequest.isAckRequested()) {
                    ackRequest.sendAckData("Joined room: " + room);
                }
            });

            // Subscribe Listener (optional, for consistency)
            server.addEventListener("subscribe", String.class, (client, channel, ackRequest) -> {
                log.info("Client {} subscribed to channel: {}", client.getSessionId(), channel);

                if (ackRequest.isAckRequested()) {
                    ackRequest.sendAckData("Subscribed to: " + channel);
                }
            });

            log.info("SocketIO server started at port: {}", server.getConfiguration().getPort());
        } catch (Exception e) {
            log.error("Could not start SocketIO server", e);
        }
    }

    @PreDestroy
    public void stopServer() {
        server.stop();
        log.info("SocketIO server stopped");
    }

    public void sendOrderCreated(Order order) {
        OrderSocketDTO dto = convertToDTO(order);
        log.info("Emitting 'order_created' to 'admin' room for order ID: {}", order.getId());
        server.getRoomOperations("admin").sendEvent("order_created", dto);
    }

    public void sendOrderUpdated(Order order) {
        // 1. Send full details to ADMIN
        OrderSocketDTO adminDto = convertToDTO(order);
        log.info("Emitting 'order_updated' to 'admin' room for order ID: {}", order.getId());
        server.getRoomOperations("admin").sendEvent("order_updated", adminDto);

        // 2. Send masked details to CUSTOMER (Tracking Room)
        if (order.getUuid() != null) {
            com.example.pizza.dto.order.OrderTrackingResponse trackingDto = orderMapper.toTrackingResponse(order);
            String roomName = "order_" + order.getUuid().toString();
            log.info("Emitting 'order_updated' to '{}' room", roomName);
            server.getRoomOperations(roomName).sendEvent("order_updated", trackingDto);
        }
    }

    private OrderSocketDTO convertToDTO(Order order) {
        return OrderSocketDTO.builder()
                .id(order.getId())
                .orderNumber(order.getId() != null ? "ORD-" + order.getId() : null)
                .orderStatus(order.getOrderStatus())
                .totalAmount(order.getTotalAmount())
                .customerName(order.getUser() != null ? order.getUser().getName()
                        : (order.getDeliveryAddress() != null ? order.getDeliveryAddress().getRecipientName()
                                : "Guest"))
                .customerEmail(order.getOrderEmail())
                .orderDate(order.getOrderDate())
                .itemCount(order.getItems() != null ? order.getItems().size() : 0)
                .items(order.getItems() != null ? order.getItems().stream()
                        .map(item -> OrderSocketDTO.OrderItemSocketDTO.builder()
                                .productId(item.getProduct().getId())
                                .productName(item.getProduct().getName())
                                .quantity(item.getQuantity())
                                .price(item.getPrice())
                                .build())
                        .collect(Collectors.toList()) : null)
                .build();
    }
}

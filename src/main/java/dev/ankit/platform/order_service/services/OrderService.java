package dev.ankit.platform.order_service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ankit.platform.order_service.domain.Order;
import dev.ankit.platform.order_service.domain.OrderStatus;
import dev.ankit.platform.order_service.dto.CreateOrderRequest;
import dev.ankit.platform.order_service.dto.OrderResponse;
import dev.ankit.platform.order_service.exception.OrderNotFoundException;
import dev.ankit.platform.order_service.outbox.EventType;
import dev.ankit.platform.order_service.outbox.OrderOutbox;
import dev.ankit.platform.order_service.outbox.OutboxStatus;
import dev.ankit.platform.order_service.repository.OrderOutboxRepository;
import dev.ankit.platform.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper; // Spring Boot auto-configured

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = Order.builder()
                .userId(request.getUserId())
                .totalAmount(request.getTotalAmount())
                .status(OrderStatus.CREATED)
                .build();

        Order saved = orderRepository.save(order);

        // ✅ Outbox event (Kafka-ready)
        Map<String, Object> eventPayload = new LinkedHashMap<>();
        eventPayload.put("orderId", saved.getId());
        eventPayload.put("userId", saved.getUserId());
        eventPayload.put("totalAmount", saved.getTotalAmount());
        eventPayload.put("status", saved.getStatus());
        eventPayload.put("eventTime", System.currentTimeMillis());

        String payloadJson = toJson(eventPayload);

        OrderOutbox outbox = OrderOutbox.builder()
                .aggregateId(saved.getId())
                .eventType(EventType.ORDER_CREATED.name())
                .payload(payloadJson)
                .status(OutboxStatus.NEW)
                .build();

        outboxRepository.save(outbox);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(UUID userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize outbox payload", e);
        }
    }


    @Transactional
    public void markPaymentCompleted(UUID orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        // ✅ idempotency: if already final, ignore
        if (order.getStatus() == OrderStatus.PAYMENT_COMPLETED) {
            log.info("ℹ️ Order already PAYMENT_COMPLETED orderId={}", orderId);
            return;
        }

        order.setStatus(OrderStatus.PAYMENT_COMPLETED);
        // No explicit save() needed if entity is managed in transaction
        //transaction end pe Hibernate automatically dirty checking karta hai
        log.info("✅ Order updated to PAYMENT_COMPLETED orderId={}", orderId);
    }

    @Transactional
    public void markPaymentFailed(UUID orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        // ✅ idempotency: if already failed, ignore
        if (order.getStatus() == OrderStatus.PAYMENT_FAILED) {
            log.info("ℹ️ Order already PAYMENT_FAILED orderId={}", orderId);
            return;
        }

        order.setStatus(OrderStatus.PAYMENT_FAILED);
        // No explicit save() needed if entity is managed in transaction
        //transaction end pe Hibernate automatically dirty checking karta hai
        log.info("✅ Order updated to PAYMENT_FAILED orderId={}", orderId);
    }

}
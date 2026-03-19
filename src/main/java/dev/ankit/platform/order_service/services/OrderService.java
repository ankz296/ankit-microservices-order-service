package dev.ankit.platform.order_service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ankit.platform.order_service.client.ProductClient;
import dev.ankit.platform.order_service.client.UserClient;
import dev.ankit.platform.order_service.domain.Order;
import dev.ankit.platform.order_service.domain.OrderItem;
import dev.ankit.platform.order_service.domain.OrderStatus;
import dev.ankit.platform.order_service.dto.CreateOrderRequest;
import dev.ankit.platform.order_service.dto.OrderResponse;
import dev.ankit.platform.order_service.exception.BusinessException;
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

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper; // Spring Boot auto-configured
    private final UserClient userClient;
    private final DownstreamValidationService downstreamValidationService;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Starting order creation for userId={}, itemsCount={}",
                request.userId(),
                request.items() != null ? request.items().size() : 0);

        // 1) User validation
        UserClient.UserInternalDto user = downstreamValidationService.fetchUser(request.userId());

        if (!user.active()) {
            log.warn("User is inactive userId={}", request.userId());
            throw new BusinessException("User is not active: " + request.userId());
        }

        BigDecimal total = BigDecimal.ZERO;

        Order order = new Order();
        order.setUserId(request.userId());
        order.setStatus(OrderStatus.CREATED);
        /**
         * Example flow (our project):
         *
         * Order-service calls product-service
         * If slow → Timeout
         * Retry 3 times
         * Still fail → count failure
         * Circuit OPEN
         * Instant fallback (fail fast)
         * After 10s → HALF_OPEN
         * If healthy → CLOSED
         */
        for (CreateOrderRequest.OrderItemRequest item : request.items()) {

            log.debug("Validating product productId={}, quantity={}",
                    item.productId(), item.quantity());

            ProductClient.ProductInternalDto p =
                    downstreamValidationService.fetchProduct(item.productId());

            if (!p.available()) {
                log.warn("Product not available productId={}", item.productId());
                throw new BusinessException("Product not available: " + item.productId());
            }

            if (p.stock() == null || p.stock() < item.quantity()) {
                log.warn("Insufficient stock productId={}, stock={}, requested={}",
                        item.productId(), p.stock(), item.quantity());
                throw new BusinessException("Insufficient stock for productId=" + item.productId());
            }

            if (p.price() == null) {
                log.error("Product price missing productId={}", item.productId());
                throw new BusinessException("Product price missing for productId=" + item.productId());
            }

            OrderItem oi = new OrderItem(item.productId(), item.quantity(), p.price());
            order.addItem(oi);

            total = total.add(oi.getLineTotal());
        }

        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);

        log.info("Order persisted successfully orderId={}, totalAmount={}",
                saved.getId(), saved.getTotalAmount());

        // Outbox event
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

        log.info("Outbox event created for orderId={}, eventType=ORDER_CREATED", saved.getId());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {

        log.info("Fetching order by id={}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("Order not found orderId={}", orderId);
                    return new OrderNotFoundException(orderId);
                });

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
            throw new BusinessException("Failed to serialize outbox payload");
        }
    }


    @Transactional
    public void markPaymentCompleted(UUID orderId) {
        log.info("Processing payment success for orderId={}", orderId);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // ✅ idempotency: if already final, ignore
        if (order.getStatus() == OrderStatus.PAYMENT_COMPLETED) {
            log.info("Duplicate payment success ignored orderId={}", orderId);
            return;
        }
        order.setStatus(OrderStatus.PAYMENT_COMPLETED);
        // No explicit save() needed if entity is managed in transaction
        //transaction end pe Hibernate automatically dirty checking karta hai
        log.info("Order marked as PAYMENT_COMPLETED orderId={}", orderId);
    }

    @Transactional
    public void markPaymentFailed(UUID orderId) {
        log.info("Processing payment failure for orderId={}", orderId);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // ✅ idempotency: if already failed, ignore
        if (order.getStatus() == OrderStatus.PAYMENT_FAILED) {
            log.info("Duplicate payment failure ignored orderId={}", orderId);
            return;
        }

        order.setStatus(OrderStatus.PAYMENT_FAILED);
        // No explicit save() needed if entity is managed in transaction
        //transaction end pe Hibernate automatically dirty checking karta hai
        log.warn("Order marked as PAYMENT_FAILED orderId={}", orderId);
    }

}
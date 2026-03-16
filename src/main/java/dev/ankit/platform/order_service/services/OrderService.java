package dev.ankit.platform.order_service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ankit.platform.order_service.client.ProductClient;
import dev.ankit.platform.order_service.client.UserClient;
import dev.ankit.platform.order_service.domain.Order;
import dev.ankit.platform.order_service.domain.OrderItem;
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

        // 1) User validation
        UserClient.UserInternalDto user = downstreamValidationService.fetchUser(request.userId());

        if (!user.active()) {
            throw new IllegalArgumentException("User is not active: " + request.userId());
        }


        // 2) Product validation + pricing (choose bulk or loop)
        // Option A: loop calls (quick)
        var pricedItems = new ArrayList<ProductClient.ProductInternalDto>();

        for (CreateOrderRequest.OrderItemRequest item : request.items()) {
            ProductClient.ProductInternalDto p = downstreamValidationService.fetchProduct(item.productId());
            if (!p.available() || p.stock() == null || p.stock() < item.quantity()) {
                throw new IllegalArgumentException("Product not available/insufficient stock: " + item.productId());
            }
            pricedItems.add(new ProductClient.ProductInternalDto(item.productId(), true,item.quantity(), p.price()));
        }

        // 3) Create order + items
        Order order = new Order();
        BigDecimal total = BigDecimal.ZERO;

        for (ProductClient.ProductInternalDto pi : pricedItems) {
            OrderItem oi = new OrderItem(pi.productId(), pi.stock(), pi.price());
            order.addItem(oi);
            total = total.add(oi.getLineTotal());
        }

        order.setUserId(request.userId());
        order.setTotalAmount(total);
        order.setStatus(OrderStatus.CREATED);

        Order saved = orderRepository.save(order);
        // 4) Outbox: order.created (include items + total)
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
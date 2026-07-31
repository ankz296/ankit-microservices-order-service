package dev.ankit.platform.order_service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ankit.platform.order_service.application.command.OrderCommandService;
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
import dev.ankit.platform.order_service.persistence.routing.ShardExecutionTemplate;
import dev.ankit.platform.order_service.persistence.routing.ShardKeyResolver;
import dev.ankit.platform.order_service.repository.OrderOutboxRepository;
import dev.ankit.platform.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService implements OrderCommandService {

    private final OrderRepository orderRepository;
    private final OrderOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final UserClient userClient;
    private final DownstreamValidationService downstreamValidationService;
    private final ShardKeyResolver shardKeyResolver;
    private final ShardExecutionTemplate shardExecutionTemplate;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String userId) {
        String shardId = shardKeyResolver.resolveShard(userId);
        return shardExecutionTemplate.executeWrite(shardId, () -> {
            log.info("Starting order creation on write path shardId={}, userId={}, itemsCount={}",
                    shardId,
                    userId,
                    request.items() != null ? request.items().size() : 0);

            UserClient.UserInternalDto user = downstreamValidationService.fetchUser(UUID.fromString(userId));
            if (!user.active()) {
                log.warn("User is inactive userId={}", userId);
                throw new BusinessException("User is not active: " + userId);
            }

            BigDecimal total = BigDecimal.ZERO;
            Order order = new Order();
            order.setUserId(UUID.fromString(userId));
            order.setStatus(OrderStatus.CREATED);

            for (CreateOrderRequest.OrderItemRequest item : request.items()) {
                log.debug("Validating product productId={}, quantity={}", item.productId(), item.quantity());

                ProductClient.ProductInternalDto product =
                        downstreamValidationService.fetchProduct(item.productId());

                if (!product.available()) {
                    throw new BusinessException("Product not available: " + item.productId());
                }
                if (product.stock() == null || product.stock() < item.quantity()) {
                    throw new BusinessException("Insufficient stock for productId=" + item.productId());
                }
                if (product.price() == null) {
                    throw new BusinessException("Product price missing for productId=" + item.productId());
                }

                OrderItem orderItem = new OrderItem(item.productId(), item.quantity(), product.price());
                order.addItem(orderItem);
                total = total.add(orderItem.getLineTotal());
            }

            order.setTotalAmount(total);
            Order saved = orderRepository.save(order);

            Map<String, Object> eventPayload = new LinkedHashMap<>();
            eventPayload.put("orderId", saved.getId());
            eventPayload.put("userId", saved.getUserId());
            eventPayload.put("totalAmount", saved.getTotalAmount());
            eventPayload.put("status", saved.getStatus());
            eventPayload.put("eventTime", System.currentTimeMillis());

            OrderOutbox outbox = OrderOutbox.builder()
                    .aggregateId(saved.getId())
                    .eventType(EventType.ORDER_CREATED.name())
                    .payload(toJson(eventPayload))
                    .status(OutboxStatus.NEW)
                    .build();

            outboxRepository.save(outbox);

            log.info("Order persisted and outbox captured shardId={}, orderId={}, totalAmount={}",
                    shardId, saved.getId(), saved.getTotalAmount());
            return mapToResponse(saved);
        });
    }

    @Override
    @Transactional
    public void markPaymentCompleted(UUID orderId, String userId) {
        String shardId = shardKeyResolver.resolveShard(userId);
        shardExecutionTemplate.executeWrite(shardId, () -> {
            log.info("Processing payment success on write path shardId={}, orderId={}", shardId, orderId);
            Order order = orderRepository.findByIdForUpdate(orderId)
                    .orElseThrow(() -> new OrderNotFoundException(orderId));

            if (order.getStatus() == OrderStatus.PAYMENT_COMPLETED) {
                log.info("Duplicate payment success ignored orderId={}", orderId);
                return null;
            }

            order.setStatus(OrderStatus.PAYMENT_COMPLETED);
            log.info("Order marked as PAYMENT_COMPLETED shardId={}, orderId={}", shardId, orderId);
            return null;
        });
    }

    @Override
    @Transactional
    public void markPaymentFailed(UUID orderId, String userId) {
        String shardId = shardKeyResolver.resolveShard(userId);
        shardExecutionTemplate.executeWrite(shardId, () -> {
            log.info("Processing payment failure on write path shardId={}, orderId={}", shardId, orderId);
            Order order = orderRepository.findByIdForUpdate(orderId)
                    .orElseThrow(() -> new OrderNotFoundException(orderId));

            if (order.getStatus() == OrderStatus.PAYMENT_FAILED) {
                log.info("Duplicate payment failure ignored orderId={}", orderId);
                return null;
            }

            order.setStatus(OrderStatus.PAYMENT_FAILED);
            log.warn("Order marked as PAYMENT_FAILED shardId={}, orderId={}", shardId, orderId);
            return null;
        });
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
}

package dev.ankit.platform.order_service.application.query;

import dev.ankit.platform.order_service.domain.Order;
import dev.ankit.platform.order_service.dto.OrderResponse;
import dev.ankit.platform.order_service.exception.OrderNotFoundException;
import dev.ankit.platform.order_service.persistence.routing.OrderAdvancedTopologyProperties;
import dev.ankit.platform.order_service.persistence.routing.ShardExecutionTemplate;
import dev.ankit.platform.order_service.persistence.routing.ShardKeyResolver;
import dev.ankit.platform.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderReadService implements OrderQueryService {

    private final OrderRepository orderRepository;
    private final ShardKeyResolver shardKeyResolver;
    private final ShardExecutionTemplate shardExecutionTemplate;
    private final OrderAdvancedTopologyProperties topologyProperties;

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        for (String shardId : allShardIds()) {
            Optional<OrderResponse> maybeOrder = shardExecutionTemplate.executeRead(shardId, () -> {
                log.info("Fetching order from read path shardId={}, orderId={}", shardId, orderId);
                return orderRepository.findById(orderId).map(this::mapToResponse);
            });
            if (maybeOrder.isPresent()) {
                return maybeOrder.get();
            }
        }
        throw new OrderNotFoundException(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(String userId) {
        UUID userUuid = UUID.fromString(userId);
        String shardId = shardKeyResolver.resolveShard(userUuid);
        return shardExecutionTemplate.executeRead(shardId, () -> {
            log.info("Fetching user orders from read path shardId={}, userId={}", shardId, userId);
            return orderRepository.findByUserId(userUuid)
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
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

    private List<String> allShardIds() {
        if (!topologyProperties.getShards().isEmpty()) {
            return topologyProperties.getShards().keySet().stream().toList();
        }
        int shardCount = Math.max(topologyProperties.getShardCount(), 1);
        return IntStream.range(0, shardCount)
                .mapToObj(index -> "order-shard-" + index)
                .toList();
    }
}

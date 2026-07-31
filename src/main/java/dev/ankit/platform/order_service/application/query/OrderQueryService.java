package dev.ankit.platform.order_service.application.query;

import dev.ankit.platform.order_service.dto.OrderResponse;

import java.util.List;
import java.util.UUID;

public interface OrderQueryService {

    OrderResponse getOrder(UUID orderId);

    List<OrderResponse> getOrdersByUser(String userId);
}

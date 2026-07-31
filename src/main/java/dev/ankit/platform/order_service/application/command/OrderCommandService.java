package dev.ankit.platform.order_service.application.command;

import dev.ankit.platform.order_service.dto.CreateOrderRequest;
import dev.ankit.platform.order_service.dto.OrderResponse;

import java.util.UUID;

public interface OrderCommandService {

    OrderResponse createOrder(CreateOrderRequest request, String userId);

    void markPaymentCompleted(UUID orderId, String userId);

    void markPaymentFailed(UUID orderId, String userId);
}

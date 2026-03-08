package dev.ankit.platform.order_service.dto;

import dev.ankit.platform.order_service.domain.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private UUID orderId;
    private UUID userId;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private OffsetDateTime createdAt;
}
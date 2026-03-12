package dev.ankit.platform.order_service.dto;


import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCancelledEvent {
    String id;
    UUID orderId;
    String reason;
    Instant eventTime;
}

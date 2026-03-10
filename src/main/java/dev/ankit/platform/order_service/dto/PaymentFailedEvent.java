package dev.ankit.platform.order_service.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentFailedEvent {
    private UUID orderId;
    private UUID paymentId;
    private UUID userId;
    private BigDecimal amount;
    private String status; // FAILED
    private String reason;
    private Long eventTime;
}
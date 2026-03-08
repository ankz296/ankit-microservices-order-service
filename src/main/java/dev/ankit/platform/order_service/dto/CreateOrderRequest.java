package dev.ankit.platform.order_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {

    @NotNull(message = "userId is required")
    private UUID userId;

    @NotNull(message = "totalAmount is required")
    @Positive(message = "totalAmount must be > 0")
    private BigDecimal totalAmount;
}
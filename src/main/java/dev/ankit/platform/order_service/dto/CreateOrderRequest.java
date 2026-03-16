package dev.ankit.platform.order_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull(message = "userId is required")
        UUID userId,

        @NotEmpty(message = "items must not be empty")
        @Valid
            List<OrderItemRequest> items
) {
    public record OrderItemRequest(
            @NotNull(message = "productId is required")
            String productId,

            @NotNull(message = "quantity is required")
            @Positive(message = "quantity must be > 0")
            Integer quantity
    ) {
    }
}

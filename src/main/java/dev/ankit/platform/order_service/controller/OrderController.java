package dev.ankit.platform.order_service.controller;

import dev.ankit.platform.order_service.dto.CreateOrderRequest;
import dev.ankit.platform.order_service.dto.OrderResponse;
import dev.ankit.platform.order_service.services.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {

        log.info("Received create order request for userId={}, itemsCount={}",
                request.userId(),
                request.items() != null ? request.items().size() : 0);

        OrderResponse response = orderService.createOrder(request);

        log.info("Order created successfully with orderId={}", response.getOrderId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@PathVariable UUID id) {

        log.info("Fetching order by id={}", id);

        OrderResponse response = orderService.getOrder(id);

        log.info("Order fetched successfully for id={}", id);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> getByUser(@PathVariable UUID userId) {

        log.info("Fetching orders for userId={}", userId);

        List<OrderResponse> response = orderService.getOrdersByUser(userId);

        log.info("Fetched {} orders for userId={}", response.size(), userId);

        return ResponseEntity.ok(response);
    }
}
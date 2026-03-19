package dev.ankit.platform.order_service.services.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ankit.platform.order_service.dto.OrderCancelledEvent;
import dev.ankit.platform.order_service.dto.PaymentFailedEvent;
import dev.ankit.platform.order_service.dto.PaymentProcessedEvent;
import dev.ankit.platform.order_service.outbox.EventType;
import dev.ankit.platform.order_service.outbox.OrderOutbox;
import dev.ankit.platform.order_service.outbox.OutboxStatus;
import dev.ankit.platform.order_service.repository.OrderOutboxRepository;
import dev.ankit.platform.order_service.services.IdempotencyService;
import dev.ankit.platform.order_service.services.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentResultListener {

    private final ObjectMapper objectMapper;
    private final OrderService orderStatusService;
    private final OrderOutboxRepository outboxRepository;
    private final IdempotencyService idempotencyService;

    @KafkaListener(topics = "payment.processed", groupId = "order-service-group")
    public void onPaymentProcessed(String message) {
        try {
            PaymentProcessedEvent event =
                    objectMapper.readValue(message, PaymentProcessedEvent.class);

            String eventId = event.getEventId();

            if (eventId == null || eventId.isBlank()) {
                log.error("Missing eventId in payment.processed payload message={}", message);
                throw new IllegalArgumentException("Missing eventId");
            }

            if (!idempotencyService.tryMarkProcessed(
                    eventId,
                    "order-service.payment-processed",
                    EventType.PAYMENT_PROCESSED.name(),
                    event.getOrderId())) {

                log.info("Duplicate payment.processed ignored eventId={}", eventId);
                return;
            }

            log.info("Processing payment.processed eventId={}, orderId={}, paymentId={}",
                    eventId, event.getOrderId(), event.getPaymentId());

            orderStatusService.markPaymentCompleted(event.getOrderId());

            log.info("Order marked completed from payment event orderId={}", event.getOrderId());

        } catch (Exception e) {
            log.error("Failed to process payment.processed payload={}, error={}",
                    message, e.getMessage(), e);
            throw new RuntimeException(e); // DLQ trigger
        }
    }

    @KafkaListener(topics = "payment.failed", groupId = "order-service-group")
    public void onPaymentFailed(String message) {
        try {
            PaymentFailedEvent event =
                    objectMapper.readValue(message, PaymentFailedEvent.class);

            String eventId = event.getEventId();

            if (eventId == null || eventId.isBlank()) {
                log.error("Missing eventId in payment.failed payload message={}", message);
                throw new IllegalArgumentException("Missing eventId");
            }

            if (!idempotencyService.tryMarkProcessed(
                    eventId,
                    "order-service.payment-failed",
                    EventType.PAYMENT_FAILED.name(),
                    event.getOrderId())) {

                log.info("Duplicate payment.failed ignored eventId={}", eventId);
                return;
            }

            log.warn("Processing payment.failed eventId={}, orderId={}, reason={}",
                    eventId, event.getOrderId(), event.getReason());

            orderStatusService.markPaymentFailed(event.getOrderId());

            // create cancel event
            OrderCancelledEvent cancelledEvent = new OrderCancelledEvent(
                    String.valueOf(System.currentTimeMillis()),
                    event.getOrderId(),
                    event.getReason(),
                    Instant.now()
            );

            String payload = objectMapper.writeValueAsString(cancelledEvent);

            OrderOutbox outbox = new OrderOutbox();
            outbox.setAggregateId(event.getOrderId());
            outbox.setEventType(EventType.ORDER_CANCELLED.name());
            outbox.setPayload(payload);
            outbox.setStatus(OutboxStatus.NEW);

            outboxRepository.save(outbox);

            log.info("Order cancellation event stored in outbox orderId={}", event.getOrderId());

        } catch (Exception e) {
            log.error("Failed to process payment.failed payload={}, error={}",
                    message, e.getMessage(), e);
            throw new RuntimeException(e); // DLQ trigger
        }
    }
}
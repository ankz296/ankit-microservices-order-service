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
            PaymentProcessedEvent event = objectMapper.readValue(message, PaymentProcessedEvent.class);


            String eventId = event.getEventId();
            if (eventId == null || eventId.isBlank()) {
                throw new IllegalArgumentException("Missing eventId in payment.processed payload");
            }


            if (!idempotencyService.tryMarkProcessed(eventId, "order-service.payment-processed",EventType.PAYMENT_PROCESSED.name(), event.getOrderId())) {
                log.info("🔁 Duplicate payment.processed ignored eventId={}", eventId);
                return;
            }

            log.info("📥 Received payment.processed orderId={} paymentId={}",
                    event.getOrderId(), event.getPaymentId());

            orderStatusService.markPaymentCompleted(event.getOrderId());

        } catch (Exception e) {
            log.error("❌ Failed to process payment.processed message={}", message, e);
            throw new RuntimeException(e);
            // IMPORTANT for retry+DLQ
        }
    }

    @KafkaListener(topics = "payment.failed", groupId = "order-service-group")
    public void onPaymentFailed(String message) {
        try {
            PaymentFailedEvent event = objectMapper.readValue(message, PaymentFailedEvent.class);


            String eventId = event.getEventId();
            if (eventId == null || eventId.isBlank()) {
                throw new IllegalArgumentException("Missing eventId in payment.failed payload");
            }

            if (!idempotencyService.tryMarkProcessed(eventId, "order-service.payment-failed",EventType.PAYMENT_FAILED.name(),event.getOrderId())) {
                log.info("🔁 Duplicate payment.failed ignored eventId={}", eventId);
                return;
            }


            log.info("📥 Received payment.failed orderId={} paymentId={} reason={}",
                    event.getOrderId(), event.getPaymentId(), event.getReason());

            orderStatusService.markPaymentFailed(event.getOrderId());

            OrderCancelledEvent cancelledEvent = new OrderCancelledEvent(
                    String.valueOf(System.currentTimeMillis()),
                    event.getOrderId(),
                    event.getReason(),
                    java.time.Instant.now()
            );

            String payload = objectMapper.writeValueAsString(cancelledEvent);

            OrderOutbox outbox = new OrderOutbox();
            outbox.setAggregateId(event.getOrderId());
            outbox.setEventType(EventType.ORDER_CANCELLED.name());
            outbox.setPayload(payload);
            outbox.setStatus(OutboxStatus.NEW);

            outboxRepository.save(outbox);

            log.info("✅ ORDER_CANCELLED outbox created for orderId={}", event.getOrderId());


        } catch (Exception e) {
            log.error("❌ Failed to process payment.failed message={}", message, e);
            throw new RuntimeException(e); // ✅ IMPORTANT for retry+DLQ
        }
    }
}
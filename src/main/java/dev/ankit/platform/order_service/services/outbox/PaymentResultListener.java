package dev.ankit.platform.order_service.services.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ankit.platform.order_service.dto.PaymentFailedEvent;
import dev.ankit.platform.order_service.dto.PaymentProcessedEvent;
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

    @KafkaListener(topics = "payment.processed", groupId = "order-service-group")
    public void onPaymentProcessed(String message) {
        try {
            PaymentProcessedEvent event = objectMapper.readValue(message, PaymentProcessedEvent.class);
            log.info("📥 Received payment.processed orderId={} paymentId={}",
                    event.getOrderId(), event.getPaymentId());

            orderStatusService.markPaymentCompleted(event.getOrderId());

        } catch (Exception e) {
            log.error("❌ Failed to process payment.processed message={}", message, e);
            // Later: DLQ / retry
        }
    }

    @KafkaListener(topics = "payment.failed", groupId = "order-service-group")
    public void onPaymentFailed(String message) {
        try {
            PaymentFailedEvent event = objectMapper.readValue(message, PaymentFailedEvent.class);
            log.info("📥 Received payment.failed orderId={} paymentId={} reason={}",
                    event.getOrderId(), event.getPaymentId(), event.getReason());

            orderStatusService.markPaymentFailed(event.getOrderId());

        } catch (Exception e) {
            log.error("❌ Failed to process payment.failed message={}", message, e);
            // Later: DLQ / retry
        }
    }
}
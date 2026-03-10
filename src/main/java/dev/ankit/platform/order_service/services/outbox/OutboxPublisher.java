package dev.ankit.platform.order_service.services.outbox;


import dev.ankit.platform.order_service.outbox.OrderOutbox;
import dev.ankit.platform.order_service.outbox.OutboxStatus;
import dev.ankit.platform.order_service.repository.OrderOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private static final String TOPIC_ORDER_CREATED = "order.created";

    private final OrderOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "5000") // every 5 seconds
    @Transactional
    public void publishNewEvents() {
        List<OrderOutbox> events = outboxRepository.findByStatus(OutboxStatus.NEW);

        if (events.isEmpty()) {
            return;
        }

        log.info("📦 Outbox NEW events found: {}", events.size());

        for (OrderOutbox event : events) {
            try {
                kafkaTemplate.send(
                        TOPIC_ORDER_CREATED,
                        event.getAggregateId().toString(),   // key = orderId
                        event.getPayload()                   // value = JSON string
                );

                event.setStatus(OutboxStatus.PUBLISHED);
                log.info("✅ Published OutboxId={} aggregateId={} eventType={}",
                        event.getId(), event.getAggregateId(), event.getEventType());

            } catch (Exception ex) {
                event.setStatus(OutboxStatus.FAILED);
                log.error("❌ Publish failed OutboxId={} aggregateId={}",
                        event.getId(), event.getAggregateId(), ex);
            }
        }
    }
}

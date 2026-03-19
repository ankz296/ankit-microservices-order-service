package dev.ankit.platform.order_service.services.outbox;


import dev.ankit.platform.order_service.outbox.EventType;
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
    private static final String TOPIC_ORDER_CANCELLED = "order.cancelled";

    private final OrderOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "5000")
    @Transactional
    public void publishNewEvents() {

        List<OrderOutbox> events = outboxRepository.findByStatus(OutboxStatus.NEW);

        if (events.isEmpty()) {
            log.debug("No NEW outbox events found");
            return;
        }

        log.info("Outbox NEW events found count={}", events.size());

        for (OrderOutbox event : events) {
            try {
                String topic = resolveTopic(EventType.valueOf(event.getEventType()));

                log.info("Publishing event topic={}, orderId={}, outboxId={}",
                        topic, event.getAggregateId(), event.getId());

                kafkaTemplate.send(
                        topic,
                        event.getAggregateId().toString(),
                        event.getPayload()
                ).whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Kafka send failed topic={}, orderId={}, error={}",
                                topic, event.getAggregateId(), ex.getMessage(), ex);
                    } else {
                        log.debug("Kafka send success topic={}, partition={}, offset={}",
                                topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });

                event.setStatus(OutboxStatus.PUBLISHED);

                log.info("Outbox event marked PUBLISHED outboxId={}, orderId={}",
                        event.getId(), event.getAggregateId());

            } catch (Exception ex) {

                event.setStatus(OutboxStatus.FAILED);

                log.error("Outbox publish failed outboxId={}, orderId={}, error={}",
                        event.getId(), event.getAggregateId(), ex.getMessage(), ex);
            }
        }
    }

    private String resolveTopic(EventType eventType) {
        return switch (eventType) {
            case ORDER_CREATED -> TOPIC_ORDER_CREATED;
            case ORDER_CANCELLED -> TOPIC_ORDER_CANCELLED;
            default -> throw new IllegalStateException("Unsupported eventType for outbox: " + eventType);
        };
    }
}

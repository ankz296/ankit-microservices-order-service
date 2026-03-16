package dev.ankit.platform.order_service.outbox;


import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", nullable = false, length = 80)
    private String eventId;

    @Column(name = "consumer", nullable = false, length = 100)
    private String consumer;

    @Column(name = "processed_at", nullable = false)
    private OffsetDateTime processedAt;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "order_id", nullable = false)
    private UUID orderId; // orderId

    protected ProcessedEvent() {
    }

    public ProcessedEvent(String eventId, String consumer,String eventType,UUID orderId) {
        this.eventId = eventId;
        this.consumer = consumer;
        this.eventType = eventType;
        this.orderId = orderId;
        this.processedAt = OffsetDateTime.now();
    }
}
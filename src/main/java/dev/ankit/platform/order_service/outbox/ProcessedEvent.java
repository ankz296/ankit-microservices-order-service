package dev.ankit.platform.order_service.outbox;


import jakarta.persistence.*;

import java.time.OffsetDateTime;

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

    protected ProcessedEvent() {
    }

    public ProcessedEvent(String eventId, String consumer) {
        this.eventId = eventId;
        this.consumer = consumer;
        this.processedAt = OffsetDateTime.now();
    }
}
package dev.ankit.platform.order_service.outbox;

import dev.ankit.platform.order_service.domain.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_outbox")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId; // orderId

    @Column(name = "event_type", nullable = false, length = 100)
    private OrderStatus eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload; // store JSON as String

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
        if (this.status == null) this.status = OutboxStatus.NEW;
    }
}
package dev.ankit.platform.order_service.repository;


import dev.ankit.platform.order_service.outbox.OrderOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderOutboxRepository extends JpaRepository<OrderOutbox, UUID> {
}
package dev.ankit.platform.order_service.repository;


import dev.ankit.platform.order_service.outbox.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
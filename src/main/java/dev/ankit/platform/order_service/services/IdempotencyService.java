package dev.ankit.platform.order_service.services;


import dev.ankit.platform.order_service.outbox.ProcessedEvent;
import dev.ankit.platform.order_service.repository.ProcessedEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
public class IdempotencyService {

    private final ProcessedEventRepository repository;

    public IdempotencyService(ProcessedEventRepository repository) {
        this.repository = repository;
    }

    /**
     * @return true if first time processing this eventId, false if duplicate.
     */
    @Transactional
    public boolean tryMarkProcessed(String eventId, String consumer, String eventType, UUID orderId) {
        try {
            repository.save(new ProcessedEvent(eventId, consumer, eventType, orderId));

            log.debug("Event marked processed eventId={}, consumer={}", eventId, consumer);

            return true;
        } catch (DataIntegrityViolationException dup) {
            log.info("Duplicate event detected eventId={}, consumer={}", eventId, consumer);
            return false;
        }
    }
}
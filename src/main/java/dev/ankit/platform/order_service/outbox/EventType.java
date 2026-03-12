package dev.ankit.platform.order_service.outbox;

public enum EventType {
    ORDER_CREATED,
    PAYMENT_PROCESSED,
    PAYMENT_FAILED,
    ORDER_CANCELLED
}

package dev.ankit.platform.order_service.outbox;


public enum OutboxStatus {
    NEW,
    PUBLISHED,
    FAILED
}
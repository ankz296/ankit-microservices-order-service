package dev.ankit.platform.order_service.domain;

public enum OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    CANCELLED
}
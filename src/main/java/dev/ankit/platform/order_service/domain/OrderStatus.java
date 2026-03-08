package dev.ankit.platform.order_service.domain;

public enum OrderStatus {
    ORDER_CREATED,
    PAYMENT_PENDING,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    CANCELLED
}
package com.example.notification.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Mirrors the message published by order-service.
 * Each service owns its own copy — no shared library between services.
 */
public record OrderPlacedMessage(
        Long orderId,
        Long customerId,
        String contactEmail,
        BigDecimal totalAmount,
        Instant occurredAt
) {}

package com.example.order.dto;

import com.example.order.model.Order;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Mensaje publicado a RabbitMQ cuando se crea un pedido.
 *
 * Es un record simple y serializable — no depende de la entidad Order.
 * notification-service tiene su propia copia de este contrato.
 */
public record OrderPlacedMessage(
        Long orderId,
        Long customerId,
        String contactEmail,
        BigDecimal totalAmount,
        Instant occurredAt
) {
    public static OrderPlacedMessage from(Order order) {
        return new OrderPlacedMessage(
                order.getId(),
                order.getCustomerId(),
                order.getContactEmail(),
                order.getTotalAmount(),
                Instant.now()
        );
    }
}

package com.example.order.event;

import com.example.order.model.Order;

import java.time.Instant;

/**
 * DOMAIN EVENT: representa algo que ocurrió en el dominio.
 *
 * Usamos record (Java 16+): inmutable, con constructor canónico,
 * equals/hashCode/toString automáticos. Ideal para eventos.
 *
 * Por qué eventos en vez de llamadas directas:
 *   - OrderService no necesita conocer EmailService ni InventoryService.
 *   - Agregar un nuevo listener (ej: analytics) = nueva clase, sin tocar OrderService.
 *   - PATRÓN OBSERVER / Pub-Sub dentro del proceso.
 */
public record OrderPlacedEvent(
        Order order,
        Instant occurredAt
) {
    /** Constructor de conveniencia: la hora de ocurrencia es "ahora" */
    public OrderPlacedEvent(Order order) {
        this(order, Instant.now());
    }
}

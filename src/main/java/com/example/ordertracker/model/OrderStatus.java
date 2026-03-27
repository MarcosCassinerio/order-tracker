package com.example.ordertracker.model;

/**
 * Estados posibles de un pedido.
 *
 * Transiciones válidas:
 *   PENDING → CONFIRMED → SHIPPED → DELIVERED
 *   PENDING → CANCELLED
 *   CONFIRMED → CANCELLED
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    /**
     * Verifica si la transición al nuevo estado es válida.
     * Encapsulamiento: la lógica de transiciones vive en el enum, no en el servicio.
     */
    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING    -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED  -> next == SHIPPED   || next == CANCELLED;
            case SHIPPED    -> next == DELIVERED;
            default         -> false;  // DELIVERED y CANCELLED son estados finales
        };
    }
}

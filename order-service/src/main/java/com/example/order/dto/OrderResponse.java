package com.example.order.dto;

import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import com.example.order.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * DTO de salida — lo que el cliente recibe en la respuesta.
 *
 * Nunca expongas la entidad JPA directamente porque:
 *   - Podría tener campos sensibles (que no querés exponer).
 *   - Lazy-loading de JPA puede causar LazyInitializationException fuera de la transacción.
 *   - La estructura interna de la entidad puede cambiar sin que quieras cambiar la API.
 *
 * El método estático from() actúa como mapper limpio sin dependencias externas.
 */
public record OrderResponse(
        Long id,
        Long customerId,
        String contactEmail,
        OrderStatus status,
        BigDecimal totalAmount,
        BigDecimal rawSubtotal,        // subtotal sin descuento — útil para el cliente
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {
    /** Mapper estático: Order → OrderResponse */
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getContactEmail(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getRawSubtotal(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    // ── DTO anidado para los ítems ─────────────────────────────────────────
    public record OrderItemResponse(
            Long id,
            Long productId,
            String productName,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {
        public static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(
                    item.getId(),
                    item.getProductId(),
                    item.getProductName(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.subtotal()
            );
        }
    }
}

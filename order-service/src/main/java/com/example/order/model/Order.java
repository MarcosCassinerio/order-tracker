package com.example.order.model;

import com.example.order.exception.InvalidStatusTransitionException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root del dominio de pedidos.
 *
 * PRINCIPIO DE ENCAPSULAMIENTO:
 *   - Los campos son privados y se acceden solo a través de métodos de comportamiento.
 *   - No hay setters públicos: todo cambio de estado pasa por métodos que validan invariantes.
 *   - La lista de items se expone como unmodifiable → nadie puede agregar items por fuera.
 *
 * PRINCIPIO DDD:
 *   - Order es el Aggregate Root. Toda modificación a OrderItems pasa por Order.
 *   - Los items tienen cascade ALL y orphanRemoval → Order controla su ciclo de vida.
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id")
@ToString(exclude = "items")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false, length = 100)
    private String contactEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    /**
     * Precio total DESPUÉS de aplicar descuentos.
     * Se calcula y persiste al crear el pedido para poder consultarlo
     * sin recargar todos los items cada vez.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    // ── Factory method ─────────────────────────────────────────────────────

    /**
     * Única forma válida de crear un Order.
     * Valida que haya al menos un item y que el total sea consistente.
     *
     * @param customerId    ID del cliente
     * @param contactEmail  Email para notificaciones
     * @param items         Ítems del pedido (al menos uno)
     * @param totalAmount   Total ya calculado por la PricingStrategy
     */
    public static Order create(Long customerId, String contactEmail,
                               List<OrderItem> items, BigDecimal totalAmount) {
        if (items == null || items.isEmpty())
            throw new IllegalArgumentException("Un pedido debe tener al menos un ítem");
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("El total no puede ser negativo");

        var order = new Order();
        order.customerId   = customerId;
        order.contactEmail = contactEmail;
        order.totalAmount  = totalAmount;
        order.items        = new ArrayList<>(items);
        order.items.forEach(item -> item.assignOrder(order));  // set FK so JPA inserts order_id correctly
        return order;
    }

    // ── Comportamiento ─────────────────────────────────────────────────────

    /**
     * Cambia el estado del pedido.
     * La lógica de qué transición es válida vive en el enum (SRP).
     *
     * @throws InvalidStatusTransitionException si la transición no está permitida
     */
    public void transitionTo(OrderStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(
                "No se puede pasar de %s a %s".formatted(this.status, newStatus)
            );
        }
        this.status = newStatus;
    }

    /**
     * Devuelve una vista inmutable de los items.
     * El caller no puede modificar la lista interna.
     */
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Subtotal bruto (sin descuentos) — suma de todos los items.
     * Útil para la PricingStrategy que calcula el descuento a aplicar.
     */
    public BigDecimal getRawSubtotal() {
        return items.stream()
                .map(OrderItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

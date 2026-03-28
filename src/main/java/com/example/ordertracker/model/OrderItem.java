package com.example.ordertracker.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * Entidad JPA que representa un ítem dentro de un pedido.
 *
 * Value Object en términos de DDD: no tiene identidad propia más allá del pedido.
 * Por eso la PK es generada y el cascade viene desde Order (el Aggregate Root).
 */
@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA necesita constructor sin args
@EqualsAndHashCode(of = "id")
@ToString
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    // Package-private: only Order (same package) sets this when building the aggregate
    void assignOrder(Order order) {
        this.order = order;
    }

    // Factory method: controla la creación, valida invariantes
    public static OrderItem of(String productId, String productName,
                                int quantity, BigDecimal unitPrice) {
        if (quantity <= 0)
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        if (unitPrice.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("El precio debe ser mayor a 0");

        var item = new OrderItem();
        item.productId   = productId;
        item.productName = productName;
        item.quantity    = quantity;
        item.unitPrice   = unitPrice;
        return item;
    }

    /** Subtotal de este ítem */
    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}

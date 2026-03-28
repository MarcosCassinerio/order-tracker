package com.example.inventory.repository.inventory.model;

import com.example.inventory.repository.inventory.exception.InvalidReserveAmountException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

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
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id")
@ToString
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer stock;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;


    public static Product create(String name, Integer stock) {
        if (stock < 0)
            throw new IllegalArgumentException("El stock no puede ser negativo");

        var product = new Product();
        product.name = name;
        product.stock = stock;
        return product;
    }

    public void reserveStock(Integer reserveAmount) {
        if (this.stock < reserveAmount) {
            throw new InvalidReserveAmountException(
                    "Se intento reservar %s mientras se tiene %s en stock".formatted(reserveAmount, this.stock)
            );
        }

        this.stock = this.stock - reserveAmount;
    }

    public void releaseStock(Integer releaseAmount) {
        this.stock = this.stock + releaseAmount;
    }
}

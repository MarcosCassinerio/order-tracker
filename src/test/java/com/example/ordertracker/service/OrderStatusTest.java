package com.example.ordertracker.service;

import com.example.ordertracker.model.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test del enum OrderStatus.
 *
 * Testea la lógica de transiciones de estado que vive en el enum.
 * Demuestra que la lógica de negocio puede (y debe) estar en el modelo,
 * no siempre en el servicio. (Encapsulamiento real.)
 */
@DisplayName("OrderStatus — transiciones de estado")
class OrderStatusTest {

    @ParameterizedTest(name = "{0} → {1} debe ser válido: {2}")
    @CsvSource({
            // Transiciones válidas
            "PENDING,   CONFIRMED,  true",
            "PENDING,   CANCELLED,  true",
            "CONFIRMED, SHIPPED,    true",
            "CONFIRMED, CANCELLED,  true",
            "SHIPPED,   DELIVERED,  true",
            // Transiciones inválidas
            "PENDING,   SHIPPED,    false",
            "PENDING,   DELIVERED,  false",
            "CONFIRMED, PENDING,    false",
            "CONFIRMED, DELIVERED,  false",
            "SHIPPED,   PENDING,    false",
            "SHIPPED,   CONFIRMED,  false",
            "DELIVERED, CANCELLED,  false",
            "CANCELLED, PENDING,    false",
    })
    @DisplayName("canTransitionTo()")
    void transitionValidation(OrderStatus from, OrderStatus to, boolean expectedValid) {
        assertThat(from.canTransitionTo(to)).isEqualTo(expectedValid);
    }
}

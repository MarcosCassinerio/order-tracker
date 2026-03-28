package com.example.order.repository;

import com.example.order.model.Order;
import com.example.order.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * PATRÓN REPOSITORY
 *
 * Spring Data JPA genera la implementación completa en tiempo de ejecución.
 * Solo definimos la interfaz con métodos que siguen la convención de nombres.
 *
 * Beneficios:
 *   - OrderService depende de esta interfaz, no de la implementación JPA.
 *   - En tests podemos inyectar un InMemoryOrderRepository (Fake).
 *   - Si migramos a MongoDB, solo cambiamos la implementación, no el servicio.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Spring genera: SELECT * FROM orders WHERE customer_id = ?
    List<Order> findByCustomerId(Long customerId);

    // Con paginación — devuelve Page<Order> con metadata (totalElements, totalPages)
    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    // Filtro por status
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    // Query JPQL explícita: cuando la convención de nombres no alcanza
    @Query("""
            SELECT o FROM Order o
            WHERE o.customerId = :customerId
            AND o.status = :status
            ORDER BY o.createdAt DESC
            """)
    List<Order> findByCustomerAndStatus(
            @Param("customerId") Long customerId,
            @Param("status") OrderStatus status
    );

    // Contar pedidos pendientes por cliente (útil para límites de negocio)
    long countByCustomerIdAndStatus(Long customerId, OrderStatus status);
}

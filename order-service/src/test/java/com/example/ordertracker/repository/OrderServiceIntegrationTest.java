package com.example.ordertracker.repository;

import com.example.ordertracker.dto.CreateOrderRequest;
import com.example.ordertracker.dto.CreateOrderRequest.OrderItemRequest;
import com.example.ordertracker.dto.OrderResponse;
import com.example.ordertracker.dto.UpdateStatusRequest;
import com.example.ordertracker.exception.OrderNotFoundException;
import com.example.ordertracker.model.OrderStatus;
import com.example.ordertracker.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * INTEGRATION TEST con Testcontainers.
 *
 * QUÉ SE PRUEBA AQUÍ (que los unit tests NO pueden probar):
 *   - Las queries JPA generan el SQL correcto
 *   - Las migraciones Flyway se aplican sin errores
 *   - Los índices y constraints de la DB funcionan
 *   - El cascade de OrderItems (crear/eliminar junto al Order)
 *   - Las transacciones se comportan correctamente (@Transactional rollback)
 *   - El mapeo entidad ↔ tabla es correcto (nombres de columnas, tipos)
 *
 * @SpringBootTest: levanta el ApplicationContext COMPLETO (real beans, real DB).
 * @Testcontainers: Testcontainers gestiona el ciclo de vida del container Docker.
 * @ServiceConnection: Spring Boot detecta el container y configura el DataSource
 *                     automáticamente. Sin @DynamicPropertySource manual.
 *
 * COSTO: estos tests son más lentos (segundos vs milisegundos de los unit tests).
 * Por eso van en la capa media de la pirámide — menos tests pero más cobertura real.
 */
@SpringBootTest
@Testcontainers
@Transactional  // cada test hace rollback al terminar → tests independientes entre sí
@DisplayName("OrderService + PostgreSQL — Integration Tests")
class OrderServiceIntegrationTest {

    // Testcontainers levanta un PostgreSQL real en Docker para estos tests.
    // @ServiceConnection: Spring Boot lee la URL/user/pass del container automáticamente.
    // 'static': el container se comparte entre todos los tests de la clase (más rápido).
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("orders_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired OrderService    orderService;
    @Autowired OrderRepository orderRepository;

    // ── Fixture ───────────────────────────────────────────────────────────────

    private CreateOrderRequest makeRequest(Long customerId, String pricing) {
        return new CreateOrderRequest(
                customerId,
                "cliente%d@example.com".formatted(customerId),
                List.of(
                        new OrderItemRequest("PROD-1", "Laptop", 1, new BigDecimal("999.00")),
                        new OrderItemRequest("PROD-2", "Mouse",  2, new BigDecimal("25.00"))
                ),
                pricing
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Tests de creación
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("crear pedido persiste en PostgreSQL con todos los campos")
    void createOrder_persistsToDatabase() {
        // ACT — sin mocks: servicio real + repositorio real + PostgreSQL real
        OrderResponse response = orderService.placeOrder(makeRequest(1L, "REGULAR"));

        // ASSERT — leer directo desde la DB para verificar la persistencia
        var found = orderRepository.findById(response.id()).orElseThrow();

        assertThat(found.getCustomerId()).isEqualTo(1L);
        assertThat(found.getStatus()).isEqualTo(OrderStatus.PENDING);
        // 999 + 2×25 = 1049
        assertThat(found.getTotalAmount()).isEqualByComparingTo("1049.00");
        assertThat(found.getContactEmail()).isEqualTo("cliente1@example.com");
    }

    @Test
    @DisplayName("los ítems del pedido se persisten con CASCADE")
    void createOrder_persistsItemsWithCascade() {
        OrderResponse response = orderService.placeOrder(makeRequest(1L, "REGULAR"));

        var found = orderRepository.findById(response.id()).orElseThrow();

        // Los 2 ítems deben estar en la DB (cascade funciona)
        assertThat(found.getItems()).hasSize(2);
        assertThat(found.getItems())
                .extracting(item -> item.getProductId())
                .containsExactlyInAnyOrder("PROD-1", "PROD-2");
    }

    @Test
    @DisplayName("pricing HOLIDAY aplica 20% de descuento en la DB")
    void createOrder_holidayPricingAppliesDiscountInDb() {
        // Con HOLIDAY pricing: (999 + 50) × 0.80 = 839.20
        OrderResponse response = orderService.placeOrder(makeRequest(1L, "HOLIDAY"));

        var found = orderRepository.findById(response.id()).orElseThrow();
        assertThat(found.getTotalAmount()).isEqualByComparingTo("839.20");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Tests de queries JPA
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("findByCustomerId devuelve solo los pedidos de ese cliente")
    void findByCustomerId_returnsOnlyThatCustomersOrders() {
        // Crear 2 pedidos del cliente 10 y 1 del cliente 20
        orderService.placeOrder(makeRequest(10L, "REGULAR"));
        orderService.placeOrder(makeRequest(10L, "REGULAR"));
        orderService.placeOrder(makeRequest(20L, "REGULAR"));

        // Verificar directo en el repositorio
        var ordersClient10 = orderRepository.findByCustomerId(10L);
        var ordersClient20 = orderRepository.findByCustomerId(20L);

        assertThat(ordersClient10).hasSize(2);
        assertThat(ordersClient20).hasSize(1);
        // Todos los pedidos del cliente 10 son efectivamente de ese cliente
        assertThat(ordersClient10).allMatch(o -> o.getCustomerId().equals(10L));
    }

    @Test
    @DisplayName("findByCustomerAndStatus devuelve pedidos filtrados por status")
    void findByCustomerAndStatus_filtersCorrectly() {
        // Crear un pedido y confirmarlo
        var r1 = orderService.placeOrder(makeRequest(5L, "REGULAR"));
        orderService.updateStatus(r1.id(), new UpdateStatusRequest(OrderStatus.CONFIRMED));

        // Crear otro que queda PENDING
        orderService.placeOrder(makeRequest(5L, "REGULAR"));

        var pending   = orderRepository.findByCustomerAndStatus(5L, OrderStatus.PENDING);
        var confirmed = orderRepository.findByCustomerAndStatus(5L, OrderStatus.CONFIRMED);

        assertThat(pending).hasSize(1);
        assertThat(confirmed).hasSize(1);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Tests de transiciones de estado
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("flujo completo de estados: PENDING → CONFIRMED → SHIPPED → DELIVERED")
    void fullStatusFlow_happyPath() {
        var response = orderService.placeOrder(makeRequest(1L, "REGULAR"));
        Long id = response.id();

        // Cada transición actualiza la DB y devuelve el nuevo estado
        assertThat(orderService.updateStatus(id, new UpdateStatusRequest(OrderStatus.CONFIRMED)).status())
                .isEqualTo(OrderStatus.CONFIRMED);

        assertThat(orderService.updateStatus(id, new UpdateStatusRequest(OrderStatus.SHIPPED)).status())
                .isEqualTo(OrderStatus.SHIPPED);

        assertThat(orderService.updateStatus(id, new UpdateStatusRequest(OrderStatus.DELIVERED)).status())
                .isEqualTo(OrderStatus.DELIVERED);

        // Verificar el estado final en la DB
        var finalOrder = orderRepository.findById(id).orElseThrow();
        assertThat(finalOrder.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    @DisplayName("transición inválida en DB: PENDING → DELIVERED no se persiste")
    void invalidTransition_doesNotPersist() {
        var response = orderService.placeOrder(makeRequest(1L, "REGULAR"));
        Long id = response.id();

        // Intentar transición inválida → excepción → @Transactional hace rollback
        assertThatThrownBy(() ->
                orderService.updateStatus(id, new UpdateStatusRequest(OrderStatus.DELIVERED))
        ).isInstanceOf(RuntimeException.class);

        // El estado en la DB no cambió
        var order = orderRepository.findById(id).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Tests de error handling
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("buscar pedido inexistente lanza OrderNotFoundException")
    void findById_nonExistentOrder_throwsNotFoundException() {
        assertThatThrownBy(() -> orderService.findById(999_999L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("999999");
    }

    @Test
    @DisplayName("countByCustomerIdAndStatus: conteo correcto en la DB")
    void countByCustomerAndStatus_returnsCorrectCount() {
        orderService.placeOrder(makeRequest(7L, "REGULAR"));
        orderService.placeOrder(makeRequest(7L, "REGULAR"));
        orderService.placeOrder(makeRequest(7L, "REGULAR"));

        long pendingCount = orderRepository.countByCustomerIdAndStatus(7L, OrderStatus.PENDING);
        assertThat(pendingCount).isEqualTo(3);
    }
}

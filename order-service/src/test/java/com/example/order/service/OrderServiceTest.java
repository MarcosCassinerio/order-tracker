package com.example.order.service;

import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.CreateOrderRequest.OrderItemRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.dto.UpdateStatusRequest;
import com.example.order.event.OrderPlacedEvent;
import com.example.order.exception.InvalidStatusTransitionException;
import com.example.order.exception.OrderNotFoundException;
import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import com.example.order.model.OrderStatus;
import com.example.order.pricing.PricingStrategyFactory;
import com.example.order.pricing.RegularPricing;
import com.example.order.pricing.HolidayPricing;
import com.example.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UNIT TEST de OrderService.
 *
 * QUÉ ES UN UNIT TEST:
 *   Testea UNA SOLA CLASE en aislamiento de sus dependencias.
 *   Las dependencias se reemplazan por TEST DOUBLES (mocks, stubs, fakes).
 *
 * QUÉ SE USA AQUÍ:
 *   @Mock          → TEST DOUBLE: objeto falso que registra interacciones
 *   when().thenReturn() → STUB: configura qué devuelve el mock cuando lo llaman
 *   verify()       → MOCK VERIFICATION: verifica que cierto método fue llamado
 *   ArgumentCaptor → captura el argumento que se pasó al mock para inspeccionarlo
 *   @InjectMocks   → crea OrderService e inyecta los @Mock en el constructor
 *
 * NADA AQUÍ TOCA LA BASE DE DATOS. Corre en milisegundos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService — Unit Tests")
class OrderServiceTest {

    // ── TEST DOUBLES (mocks) ──────────────────────────────────────────────────
    @Mock private OrderRepository         repository;
    @Mock private PricingStrategyFactory  pricingFactory;
    @Mock private ApplicationEventPublisher eventPublisher;

    // LA UNIDAD BAJO TEST — Mockito le inyecta los mocks de arriba
    @InjectMocks private OrderService service;

    // ── Datos de prueba reutilizables ─────────────────────────────────────────
    private CreateOrderRequest validRequest;
    private Order               savedOrder;

    @BeforeEach
    void setUp() {
        // Request válido que usamos en la mayoría de los tests
        validRequest = new CreateOrderRequest(
                1L,
                "cliente@example.com",
                List.of(
                        new OrderItemRequest("PROD-1", "Laptop", 2, new BigDecimal("500.00")),
                        new OrderItemRequest("PROD-2", "Mouse",  1, new BigDecimal("50.00"))
                ),
                "REGULAR"
        );

        // Orden persistida que el mock del repository devuelve
        // Subtotal bruto: (2 × 500) + (1 × 50) = 1050
        savedOrder = Order.create(
                1L,
                "cliente@example.com",
                List.of(
                        OrderItem.of("PROD-1", "Laptop", 2, new BigDecimal("500.00")),
                        OrderItem.of("PROD-2", "Mouse",  1, new BigDecimal("50.00"))
                ),
                new BigDecimal("1050.00")
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    // placeOrder — tests del happy path y casos de error
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("placeOrder()")
    class PlaceOrderTests {

        @Test
        @DisplayName("precio REGULAR: el total es igual al subtotal bruto sin descuento")
        void regularPricing_totalEqualSubtotal() {
            // ARRANGE
            // STUB: cuando el factory pida "REGULAR", devuelve RegularPricing real
            // Acá usamos la implementación real (no un mock) porque queremos testear
            // que la integración entre servicio y estrategia funciona correctamente.
            when(pricingFactory.get("REGULAR")).thenReturn(new RegularPricing());
            when(repository.save(any())).thenReturn(savedOrder);

            // ACT
            OrderResponse response = service.placeOrder(validRequest);

            // ASSERT — verificar el resultado
            assertThat(response.totalAmount()).isEqualByComparingTo("1050.00");
            assertThat(response.customerId()).isEqualTo(1L);
            assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        }

        @Test
        @DisplayName("precio HOLIDAY: el total tiene 20% de descuento")
        void holidayPricing_applies20PercentDiscount() {
            // ARRANGE
            var holidayRequest = new CreateOrderRequest(
                    1L, "cliente@example.com",
                    List.of(new OrderItemRequest("PROD-1", "TV", 1, new BigDecimal("1000.00"))),
                    "HOLIDAY"
            );
            var discountedOrder = Order.create(
                    1L, "cliente@example.com",
                    List.of(OrderItem.of("PROD-1", "TV", 1, new BigDecimal("1000.00"))),
                    new BigDecimal("800.00") // 1000 × 0.80
            );

            when(pricingFactory.get("HOLIDAY")).thenReturn(new HolidayPricing());
            when(repository.save(any())).thenReturn(discountedOrder);

            // ACT
            OrderResponse response = service.placeOrder(holidayRequest);

            // ASSERT
            assertThat(response.totalAmount()).isEqualByComparingTo("800.00");
        }

        @Test
        @DisplayName("se persiste el pedido en el repositorio")
        void placeOrder_savesOrderToRepository() {
            // ARRANGE
            when(pricingFactory.get(any())).thenReturn(new RegularPricing());
            when(repository.save(any())).thenReturn(savedOrder);

            // ACT
            service.placeOrder(validRequest);

            // ASSERT — MOCK VERIFICATION: verificar que save() fue llamado exactamente 1 vez
            verify(repository, times(1)).save(any(Order.class));
        }

        @Test
        @DisplayName("se publica el evento OrderPlacedEvent después de crear el pedido")
        void placeOrder_publishesOrderPlacedEvent() {
            // ARRANGE
            when(pricingFactory.get(any())).thenReturn(new RegularPricing());
            when(repository.save(any())).thenReturn(savedOrder);

            // ACT
            service.placeOrder(validRequest);

            // ASSERT — verificar que se publicó el evento (OBSERVER pattern)
            verify(eventPublisher, times(1)).publishEvent(any(OrderPlacedEvent.class));
        }

        @Test
        @DisplayName("el evento publicado contiene el pedido correcto — ArgumentCaptor")
        void placeOrder_eventContainsCorrectOrder() {
            // ARRANGE
            when(pricingFactory.get(any())).thenReturn(new RegularPricing());
            when(repository.save(any())).thenReturn(savedOrder);

            // ACT
            service.placeOrder(validRequest);

            // ASSERT — ArgumentCaptor: capturamos lo que se pasó a publishEvent
            // para verificar que el evento contiene los datos correctos
            var captor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());

            OrderPlacedEvent publishedEvent = captor.getValue();
            assertThat(publishedEvent.order().getCustomerId()).isEqualTo(1L);
            assertThat(publishedEvent.occurredAt()).isNotNull();
        }

        @Test
        @DisplayName("si el repository falla, NO se publica el evento")
        void placeOrder_whenRepositoryFails_doesNotPublishEvent() {
            // ARRANGE
            when(pricingFactory.get(any())).thenReturn(new RegularPricing());
            // STUB: simular que el repository lanza una excepción
            when(repository.save(any())).thenThrow(new RuntimeException("DB connection lost"));

            // ACT + ASSERT
            assertThatThrownBy(() -> service.placeOrder(validRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("DB connection lost");

            // El evento NUNCA debe publicarse si la persistencia falló
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("el pedido se crea en estado PENDING siempre")
        void placeOrder_orderAlwaysStartsPending() {
            // ARRANGE
            when(pricingFactory.get(any())).thenReturn(new RegularPricing());

            // STUB con thenAnswer: devolvemos el mismo objeto que se pasó a save()
            // (simula que el repo asignó el ID y devolvió el objeto guardado)
            when(repository.save(any(Order.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // ACT
            OrderResponse response = service.placeOrder(validRequest);

            // ASSERT
            assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // updateStatus — transiciones de estado
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatusTests {

        @Test
        @DisplayName("transición válida PENDING → CONFIRMED funciona correctamente")
        void validTransition_pendingToConfirmed() {
            // ARRANGE
            // El pedido comienza en PENDING
            when(repository.findById(1L)).thenReturn(Optional.of(savedOrder));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var req = new UpdateStatusRequest(OrderStatus.CONFIRMED);

            // ACT
            OrderResponse response = service.updateStatus(1L, req);

            // ASSERT
            assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
        }

        @Test
        @DisplayName("transición inválida PENDING → DELIVERED lanza excepción")
        void invalidTransition_throwsException() {
            // ARRANGE
            when(repository.findById(1L)).thenReturn(Optional.of(savedOrder));

            // Intentamos saltar de PENDING directamente a DELIVERED (inválido)
            var req = new UpdateStatusRequest(OrderStatus.DELIVERED);

            // ACT + ASSERT
            assertThatThrownBy(() -> service.updateStatus(1L, req))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("PENDING")
                    .hasMessageContaining("DELIVERED");

            // No se guardó nada — la excepción cortó el flujo
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("actualizar estado de pedido inexistente lanza OrderNotFoundException")
        void orderNotFound_throwsException() {
            // ARRANGE — STUB: el repositorio no encuentra el pedido
            when(repository.findById(99L)).thenReturn(Optional.empty());

            var req = new UpdateStatusRequest(OrderStatus.CONFIRMED);

            // ACT + ASSERT
            assertThatThrownBy(() -> service.updateStatus(99L, req))
                    .isInstanceOf(OrderNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // findById — tests de consulta
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("retorna el pedido mapeado a DTO cuando existe")
        void existingOrder_returnsMappedDto() {
            // ARRANGE
            when(repository.findById(1L)).thenReturn(Optional.of(savedOrder));

            // ACT
            OrderResponse response = service.findById(1L);

            // ASSERT
            assertThat(response.customerId()).isEqualTo(1L);
            assertThat(response.contactEmail()).isEqualTo("cliente@example.com");
            assertThat(response.items()).hasSize(2);
        }

        @Test
        @DisplayName("lanza OrderNotFoundException cuando el pedido no existe")
        void missingOrder_throwsNotFoundException() {
            // ARRANGE
            when(repository.findById(42L)).thenReturn(Optional.empty());

            // ACT + ASSERT
            assertThatThrownBy(() -> service.findById(42L))
                    .isInstanceOf(OrderNotFoundException.class)
                    .hasMessageContaining("42");

            // El repositorio fue consultado exactamente 1 vez
            verify(repository, times(1)).findById(42L);
        }
    }
}

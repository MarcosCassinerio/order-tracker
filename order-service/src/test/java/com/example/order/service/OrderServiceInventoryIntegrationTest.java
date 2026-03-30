package com.example.order.service;

import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.CreateOrderRequest.OrderItemRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.exception.InventoryServiceException;
import com.example.order.exception.InventoryUnavailableException;
import com.example.order.model.OrderStatus;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@Transactional
@DisplayName("OrderService + Inventory WebClient — WireMock Tests")
class OrderServiceInventoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("orders_test")
            .withUsername("test")
            .withPassword("test");

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void overrideInventoryUrl(DynamicPropertyRegistry registry) {
        registry.add("services.inventory.url", wireMock::baseUrl);
    }

    @MockBean  RabbitTemplate rabbitTemplate;
    @Autowired OrderService   orderService;

    // ══════════════════════════════════════════════════════════════════════════
    // Happy path
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("inventory acepta todas las reservas → pedido creado en PENDING")
    void placeOrder_allReservesSucceed_orderCreated() {
        wireMock.stubFor(patch(urlPathMatching("/api/v1/products/.*/reserve"))
                .willReturn(aResponse().withStatus(200)));

        OrderResponse response = orderService.placeOrder(validRequest());

        assertThat(response.id()).isNotNull();
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.items()).hasSize(2);
    }

    @Test
    @DisplayName("inventory acepta reservas → se llamó reserve por cada ítem")
    void placeOrder_allReservesSucceed_reserveCalledPerItem() {
        wireMock.stubFor(patch(urlPathMatching("/api/v1/products/.*/reserve"))
                .willReturn(aResponse().withStatus(200)));

        orderService.placeOrder(validRequest());

        wireMock.verify(1, patchRequestedFor(urlEqualTo("/api/v1/products/1/reserve")));
        wireMock.verify(1, patchRequestedFor(urlEqualTo("/api/v1/products/2/reserve")));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Inventory failure
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("inventory devuelve 409 → InventoryUnavailableException")
    void placeOrder_inventoryReturns409_throwsException() {
        wireMock.stubFor(patch(urlPathMatching("/api/v1/products/.*/reserve"))
                .willReturn(aResponse().withStatus(409)));

        assertThatThrownBy(() -> orderService.placeOrder(validRequest()))
                .isInstanceOf(InventoryUnavailableException.class);
    }

    @Test
    @DisplayName("inventory devuelve 409 → pedido NO se persiste")
    void placeOrder_inventoryReturns409_orderNotPersisted() {
        wireMock.stubFor(patch(urlPathMatching("/api/v1/products/.*/reserve"))
                .willReturn(aResponse().withStatus(409)));

        assertThatThrownBy(() -> orderService.placeOrder(validRequest()))
                .isInstanceOf(InventoryUnavailableException.class);

        wireMock.verify(0, patchRequestedFor(urlPathMatching("/api/v1/products/.*/release")));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Rollback
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("segundo ítem falla → release llamado para el primero")
    void placeOrder_secondItemFails_releasesFirstItem() {
        wireMock.stubFor(patch(urlEqualTo("/api/v1/products/1/reserve"))
                .willReturn(aResponse().withStatus(200)));
        wireMock.stubFor(patch(urlEqualTo("/api/v1/products/2/reserve"))
                .willReturn(aResponse().withStatus(409)));
        wireMock.stubFor(patch(urlEqualTo("/api/v1/products/1/release"))
                .willReturn(aResponse().withStatus(200)));

        assertThatThrownBy(() -> orderService.placeOrder(validRequest()))
                .isInstanceOf(InventoryUnavailableException.class);

        wireMock.verify(1, patchRequestedFor(urlEqualTo("/api/v1/products/1/release")));
        wireMock.verify(0, patchRequestedFor(urlEqualTo("/api/v1/products/2/release")));
    }

    @Test
    @DisplayName("inventory service caído (503) → rollback y InventoryServiceException")
    void placeOrder_inventoryDown_throwsException() {
        wireMock.stubFor(patch(urlPathMatching("/api/v1/products/.*/reserve"))
                .willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> orderService.placeOrder(validRequest()))
                .isInstanceOf(InventoryServiceException.class);
    }

    // ── Fixture ───────────────────────────────────────────────────────────────

    private CreateOrderRequest validRequest() {
        return new CreateOrderRequest(
                1L,
                "cliente@example.com",
                List.of(
                        new OrderItemRequest(1L, "Laptop", 2, new BigDecimal("500.00")),
                        new OrderItemRequest(2L, "Mouse",  1, new BigDecimal("50.00"))
                ),
                "REGULAR"
        );
    }
}

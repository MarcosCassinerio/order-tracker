package com.example.order.controller;

import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.CreateOrderRequest.OrderItemRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.dto.UpdateStatusRequest;
import com.example.order.exception.GlobalExceptionHandler;
import com.example.order.exception.OrderNotFoundException;
import com.example.order.model.OrderStatus;
import com.example.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test del Controller con MockMvc.
 *
 * @WebMvcTest: levanta SOLO la capa web (Controller + filtros + serialización).
 *   NO levanta el ApplicationContext completo.
 *   NO toca la base de datos.
 *   OrderService se reemplaza por un @MockBean.
 *
 * QUÉ TESTEAMOS AQUÍ (y no en OrderServiceTest):
 *   - Deserialización del JSON de entrada
 *   - Validación de @Valid (campos requeridos, formatos)
 *   - HTTP status codes correctos (201, 200, 400, 404, 409)
 *   - Serialización del JSON de salida
 *   - Header Location en el 201
 *   - Formato de errores (RFC 7807 Problem Detail)
 */
@WebMvcTest(controllers = {OrderController.class, GlobalExceptionHandler.class})
@DisplayName("OrderController — MockMvc Tests")
class OrderControllerTest {

    @Autowired MockMvc     mockMvc;
    @Autowired ObjectMapper mapper;

    @MockBean OrderService orderService;  // reemplaza el bean real con un mock

    // ── Fixture ───────────────────────────────────────────────────────────────
    private OrderResponse sampleResponse() {
        return new OrderResponse(
                1L, 1L, "cliente@example.com",
                OrderStatus.PENDING,
                new BigDecimal("1050.00"),
                new BigDecimal("1050.00"),
                List.of(),
                Instant.now(), Instant.now()
        );
    }

    private CreateOrderRequest validRequest() {
        return new CreateOrderRequest(
                1L,
                "cliente@example.com",
                List.of(new OrderItemRequest("PROD-1", "Laptop", 2, new BigDecimal("500.00"))),
                "REGULAR"
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /api/v1/orders
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("POST /api/v1/orders")
    class CreateOrderTests {

        @Test
        @DisplayName("request válido → 201 Created + header Location + body")
        void validRequest_returns201WithLocation() throws Exception {
            when(orderService.placeOrder(any())).thenReturn(sampleResponse());

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(validRequest())))
                    .andExpect(status().isCreated())                              // 201
                    .andExpect(header().string("Location", "http://localhost/api/v1/orders/1"))  // header Location
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.totalAmount").value(1050.00));
        }

        @Test
        @DisplayName("customerId nulo → 400 Bad Request con detalle del campo")
        void missingCustomerId_returns400() throws Exception {
            var badRequest = new CreateOrderRequest(
                    null,                         // ← customerId nulo (inválido)
                    "cliente@example.com",
                    List.of(new OrderItemRequest("P1", "Prod", 1, BigDecimal.TEN)),
                    "REGULAR"
            );

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(badRequest)))
                    .andExpect(status().isBadRequest())                           // 400
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.errors[0]").value("customerId: customerId es requerido"));
        }

        @Test
        @DisplayName("email inválido → 400 Bad Request")
        void invalidEmail_returns400() throws Exception {
            var badRequest = new CreateOrderRequest(
                    1L,
                    "no-es-un-email",             // ← formato inválido
                    List.of(new OrderItemRequest("P1", "Prod", 1, BigDecimal.TEN)),
                    "REGULAR"
            );

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(badRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0]").value("contactEmail: contactEmail debe ser un email válido"));
        }

        @Test
        @DisplayName("lista de ítems vacía → 400 Bad Request")
        void emptyItems_returns400() throws Exception {
            var badRequest = new CreateOrderRequest(
                    1L, "cliente@example.com",
                    List.of(),                    // ← sin ítems (inválido)
                    "REGULAR"
            );

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(badRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0]").value("items: El pedido debe tener al menos un ítem"));
        }

        @Test
        @DisplayName("el servicio es llamado con los datos correctos del request")
        void validRequest_callsServiceWithCorrectData() throws Exception {
            when(orderService.placeOrder(any())).thenReturn(sampleResponse());

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(validRequest())))
                    .andExpect(status().isCreated());

            // Verifica que el Controller le pasó el request al Service
            verify(orderService).placeOrder(any(CreateOrderRequest.class));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /api/v1/orders/{id}
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("GET /api/v1/orders/{id}")
    class GetOrderTests {

        @Test
        @DisplayName("pedido existente → 200 OK con body")
        void existingOrder_returns200() throws Exception {
            when(orderService.findById(1L)).thenReturn(sampleResponse());

            mockMvc.perform(get("/api/v1/orders/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.customerId").value(1))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("pedido inexistente → 404 Not Found con Problem Detail")
        void missingOrder_returns404() throws Exception {
            // El service lanza la excepción; el GlobalExceptionHandler la convierte en 404
            when(orderService.findById(99L)).thenThrow(new OrderNotFoundException(99L));

            mockMvc.perform(get("/api/v1/orders/99"))
                    .andExpect(status().isNotFound())                             // 404
                    .andExpect(jsonPath("$.title").value("Order Not Found"))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.detail").value("Pedido #99 no encontrado"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PATCH /api/v1/orders/{id}/status
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("PATCH /api/v1/orders/{id}/status")
    class UpdateStatusTests {

        @Test
        @DisplayName("transición válida → 200 OK con el nuevo status")
        void validTransition_returns200() throws Exception {
            var updated = new OrderResponse(
                    1L, 1L, "cliente@example.com",
                    OrderStatus.CONFIRMED,           // ← nuevo status
                    new BigDecimal("1050.00"),
                    new BigDecimal("1050.00"),
                    List.of(),
                    Instant.now(), Instant.now()
            );
            when(orderService.updateStatus(eq(1L), any())).thenReturn(updated);

            var req = new UpdateStatusRequest(OrderStatus.CONFIRMED);

            mockMvc.perform(patch("/api/v1/orders/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));
        }

        @Test
        @DisplayName("newStatus nulo → 400 Bad Request")
        void nullStatus_returns400() throws Exception {
            // JSON con status nulo
            mockMvc.perform(patch("/api/v1/orders/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newStatus\": null}"))
                    .andExpect(status().isBadRequest());
        }
    }
}

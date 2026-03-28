package com.example.order.controller;

import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.dto.UpdateStatusRequest;
import com.example.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Controller REST para pedidos.
 *
 * RESPONSABILIDADES DE ESTA CAPA (y solo estas):
 *   1. Recibir el request HTTP y deserializar el body
 *   2. Validar la entrada con @Valid (delega a Bean Validation)
 *   3. Llamar al servicio
 *   4. Construir la respuesta HTTP con el status code correcto
 *
 * Lo que el controller NO hace:
 *   - Lógica de negocio (eso es del Service)
 *   - Acceso a datos (eso es del Repository)
 *   - Manejo de errores (eso es del GlobalExceptionHandler)
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * POST /api/v1/orders
     * → 201 Created + header Location: /api/v1/orders/{id}
     *
     * Por qué 201 y no 200:
     *   - 201 indica que se CREÓ un recurso nuevo.
     *   - El header Location le dice al cliente dónde encontrar el recurso creado.
     *   - Es la semántica correcta de REST para POST que crea.
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest req,
            UriComponentsBuilder uriBuilder) {

        OrderResponse response = orderService.placeOrder(req);

        URI location = uriBuilder
                .path("/api/v1/orders/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    /**
     * GET /api/v1/orders/{id}
     * → 200 OK o 404 (lanzado por el service, capturado por GlobalExceptionHandler)
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.findById(id));
    }

    /**
     * GET /api/v1/orders?page=0&size=20&sort=createdAt,desc
     * → 200 OK con Page<OrderResponse>
     */
    @GetMapping
    public Page<OrderResponse> listOrders(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return orderService.findAll(pageable);
    }

    /**
     * GET /api/v1/orders?customerId=42
     * → 200 OK con pedidos del cliente
     */
    @GetMapping(params = "customerId")
    public Page<OrderResponse> listByCustomer(
            @RequestParam Long customerId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return orderService.findByCustomer(customerId, pageable);
    }

    /**
     * PATCH /api/v1/orders/{id}/status
     * → 200 OK con el pedido actualizado
     *
     * Por qué PATCH y no PUT:
     *   - PUT reemplaza el recurso COMPLETO (el cliente manda toda la representación).
     *   - PATCH modifica PARCIALMENTE (solo el campo status).
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest req) {

        return ResponseEntity.ok(orderService.updateStatus(id, req));
    }
}

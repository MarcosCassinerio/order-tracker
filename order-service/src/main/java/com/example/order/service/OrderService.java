package com.example.order.service;

import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.dto.StockReservationRequest;
import com.example.order.dto.UpdateStatusRequest;
import com.example.order.config.RabbitMQConfig;
import com.example.order.dto.OrderPlacedMessage;
import com.example.order.exception.InventoryServiceException;
import com.example.order.exception.InventoryUnavailableException;
import com.example.order.exception.OrderNotFoundException;
import com.example.order.model.Order;
import com.example.order.model.OrderItem;
import com.example.order.pricing.PricingStrategyFactory;
import com.example.order.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Lógica de negocio de pedidos.
 *
 * PRINCIPIOS DEMOSTRADOS AQUÍ:
 *
 * 1. DEPENDENCY INVERSION: depende de interfaces (OrderRepository, ApplicationEventPublisher),
 *    no de implementaciones concretas.
 *
 * 2. INYECCIÓN POR CONSTRUCTOR: campos final (inmutables), dependencias explícitas,
 *    fácil de testear (inyectás mocks en el constructor sin Spring).
 *
 * 3. STRATEGY: delega el cálculo de precios a PricingStrategyFactory.
 *    El servicio no sabe si el descuento es 0%, 20% o 50%.
 *
 * 4. OBSERVER: publica OrderPlacedEvent → los listeners reaccionan de forma desacoplada.
 *    El servicio no sabe que existe OrderNotificationListener.
 *
 * 5. SINGLE RESPONSIBILITY: este servicio solo orquesta la creación/actualización de pedidos.
 *    No envía emails, no calcula descuentos directamente, no genera reportes.
 */
@Service
@Slf4j
@Transactional(readOnly = true)  // las lecturas son read-only por defecto (performance)
public class OrderService {

    // Dependencias declaradas final: inmutables, explícitas, testeables
    private final OrderRepository repository;
    private final PricingStrategyFactory pricingFactory;
    private final RabbitTemplate rabbitTemplate;
    private final WebClient inventoryClient;

    // Spring inyecta automáticamente los beans que implementan estas interfaces
    public OrderService(OrderRepository repository,
                        PricingStrategyFactory pricingFactory,
                        RabbitTemplate rabbitTemplate,
                        WebClient inventoryClient) {
        this.repository     = repository;
        this.pricingFactory = pricingFactory;
        this.rabbitTemplate = rabbitTemplate;
        this.inventoryClient = inventoryClient;
    }

    // ── Comandos (escritura) ───────────────────────────────────────────────

    /**
     * Crea un nuevo pedido.
     *
     * Flujo:
     *   1. Mapear DTO → domain objects (OrderItems)
     *   2. Calcular precio con la estrategia correcta (Strategy)
     *   3. Crear el Aggregate Root (Order.create valida invariantes)
     *   4. Persistir
     *   5. Publicar evento (Observer) — los listeners reaccionan async
     *
     * @Transactional: toda la operación es atómica.
     * Si falla el save o el publish, hace rollback.
     */
    @Transactional
    public OrderResponse placeOrder(CreateOrderRequest req) {
        log.info("Creando pedido para cliente {} con pricing {}",
                req.customerId(), req.pricingType());

        List<OrderItem> items = req.items().stream()
                .map(i -> OrderItem.of(
                        i.productId(),
                        i.productName(),
                        i.quantity(),
                        i.unitPrice()))
                .toList();

        List<OrderItem> reservedItems = new ArrayList<>();

        for (OrderItem item : items) {
            try {
                inventoryClient.patch()
                        .uri("/api/v1/products/{id}/reserve", item.getProductId())
                        .bodyValue(new StockReservationRequest(item.getQuantity()))
                        .retrieve()
                        .bodyToMono(Void.class)
                        .block();
                reservedItems.add(item);
            } catch (WebClientResponseException e) {
                releaseAll(reservedItems);
                if (e.getStatusCode() == org.springframework.http.HttpStatus.CONFLICT) {
                    throw new InventoryUnavailableException(item.getProductId());
                }
                throw new InventoryServiceException(item.getProductId(), e);
            } catch (Exception e) {
                releaseAll(reservedItems);
                throw new InventoryServiceException(item.getProductId(), e);
            }
        }

        BigDecimal rawSubtotal = items.stream()
                .map(OrderItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAmount = pricingFactory
                .get(req.pricingType())
                .calculate(rawSubtotal);

        Order order = Order.create(
                req.customerId(),
                req.contactEmail(),
                items,
                totalAmount
        );

        Order saved = repository.save(order);
        log.info("Pedido #{} creado. Subtotal: {} → Total con descuento: {}",
                saved.getId(), rawSubtotal, totalAmount);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                OrderPlacedMessage.from(saved));

        return OrderResponse.from(saved);
    }

    /**
     * Actualiza el estado de un pedido.
     * La lógica de transiciones válidas vive en OrderStatus.canTransitionTo() (SRP).
     */
    @Transactional
    public OrderResponse updateStatus(Long orderId, UpdateStatusRequest req) {
        Order order = findOrThrow(orderId);

        // Order.transitionTo() valida la transición y lanza excepción si es inválida
        order.transitionTo(req.newStatus());

        Order saved = repository.save(order);
        log.info("Pedido #{} → {}", orderId, req.newStatus());
        return OrderResponse.from(saved);
    }

    // ── Queries (lectura) ──────────────────────────────────────────────────

    public OrderResponse findById(Long orderId) {
        return OrderResponse.from(findOrThrow(orderId));
    }

    public Page<OrderResponse> findByCustomer(Long customerId, Pageable pageable) {
        return repository.findByCustomerId(customerId, pageable)
                .map(OrderResponse::from);
    }

    public Page<OrderResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(OrderResponse::from);
    }

    // ── Helper privado ─────────────────────────────────────────────────────

    private void releaseAll(List<OrderItem> reservedItems) {
        reservedItems.forEach(reserved ->
                inventoryClient.patch()
                        .uri("/api/v1/products/{id}/release", reserved.getProductId())
                        .bodyValue(new StockReservationRequest(reserved.getQuantity()))
                        .retrieve()
                        .bodyToMono(Void.class)
                        .block()
        );
    }

    /**
     * Fail fast: lanzar excepción con tipo específico si el pedido no existe.
     * El @ControllerAdvice la captura y devuelve 404.
     */
    private Order findOrThrow(Long orderId) {
        return repository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}

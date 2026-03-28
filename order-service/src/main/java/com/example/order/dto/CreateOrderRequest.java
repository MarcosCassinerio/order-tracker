package com.example.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de entrada para crear un pedido.
 *
 * Por qué no usar la entidad Order directamente:
 *   1. SEGURIDAD: evita mass assignment (el cliente no puede enviar 'id' ni 'status').
 *   2. FLEXIBILIDAD: podés cambiar la entidad sin romper el contrato de la API.
 *   3. VALIDACIÓN: las constraints de Bean Validation viven en el DTO, no en la entidad.
 *
 * record (Java 16+): constructor canónico, getters, equals, hashCode, toString
 * automáticos. Inmutable por diseño — ideal para DTOs de entrada.
 */
public record CreateOrderRequest(

        @NotNull(message = "customerId es requerido")
        @Positive(message = "customerId debe ser positivo")
        Long customerId,

        @NotBlank(message = "contactEmail es requerido")
        @Email(message = "contactEmail debe ser un email válido")
        String contactEmail,

        @NotEmpty(message = "El pedido debe tener al menos un ítem")
        @Valid   // activa las validaciones dentro de cada OrderItemRequest
        List<OrderItemRequest> items,

        /**
         * Tipo de precio a aplicar: REGULAR, HOLIDAY, VIP, etc.
         * Si no se envía, el servicio usa REGULAR como default.
         */
        String pricingType
) {
    /** record con compact constructor: normaliza pricingType */
    public CreateOrderRequest {
        pricingType = (pricingType == null || pricingType.isBlank()) ? "REGULAR" : pricingType.toUpperCase();
    }

    // ── DTO anidado para los ítems ─────────────────────────────────────────
    public record OrderItemRequest(

            @NotBlank(message = "productId es requerido")
            String productId,

            @NotBlank(message = "productName es requerido")
            String productName,

            @NotNull
            @Positive(message = "La cantidad debe ser mayor a 0")
            Integer quantity,

            @NotNull
            @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
            BigDecimal unitPrice
    ) {}
}

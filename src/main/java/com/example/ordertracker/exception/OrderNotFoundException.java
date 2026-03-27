package com.example.ordertracker.exception;

// ── Excepciones de negocio ─────────────────────────────────────────────────
// Por qué excepciones custom en vez de RuntimeException genérica:
//   - El @ControllerAdvice puede atraparlas por tipo y devolver el HTTP status correcto.
//   - El nombre documenta QUÉ salió mal, no solo "algo falló".
//   - Son parte del contrato del servicio — documentan los casos de error posibles.

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long id) {
        super("Pedido #%d no encontrado".formatted(id));
    }
}

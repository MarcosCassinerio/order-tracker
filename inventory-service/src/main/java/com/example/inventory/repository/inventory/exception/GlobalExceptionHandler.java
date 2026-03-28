package com.example.inventory.repository.inventory.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;

/**
 * Manejo centralizado de errores — PATRÓN: todas las excepciones llegan aquí.
 *
 * Por qué @RestControllerAdvice en vez de try-catch en cada controller:
 *   - DRY: la lógica de mapear excepción → HTTP status vive en un solo lugar.
 *   - SRP: el controller solo maneja el happy path; el error handling está separado.
 *   - RFC 7807: usamos ProblemDetail (Spring 6+) para respuestas de error estándar.
 *
 * Formato de respuesta de error (RFC 7807):
 *   {
 *     "type":   "https://api.example.com/errors/order-not-found",
 *     "title":  "Order Not Found",
 *     "status": 404,
 *     "detail": "Pedido #42 no encontrado"
 *   }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 400 — Validación de @Valid (campos del DTO)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> "%s: %s".formatted(e.getField(), e.getDefaultMessage()))
                .toList();

        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        pd.setTitle("Validation Error");
        pd.setProperty("errors", errors);  // lista de errores por campo
        return ResponseEntity.badRequest().body(pd);
    }

    // 404 — Producto no encontrado
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleProductNotFound(ProductNotFoundException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Product Not Found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    // 409 — Stock insuficiente
    @ExceptionHandler(InvalidReserveAmountException.class)
    public ResponseEntity<ProblemDetail> handleInvalidReserve(InvalidReserveAmountException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Insufficient Stock");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    // 400 — Argumentos inválidos de negocio
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Invalid Argument");
        return ResponseEntity.badRequest().body(pd);
    }

    // 500 — Catchall para errores inesperados
    // NUNCA exponer el stack trace al cliente — solo loguear y devolver mensaje genérico
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex) {
        // En producción: loguear con traceId para correlacionar en Grafana
        var pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado. Contacte al soporte."
        );
        pd.setTitle("Internal Server Error");
        return ResponseEntity.internalServerError().body(pd);
    }
}

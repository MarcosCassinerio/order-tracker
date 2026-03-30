package com.example.order.exception;

public class InventoryServiceException extends RuntimeException {
    public InventoryServiceException(Long productId, Throwable cause) {
        super("Error al contactar inventory-service para el producto #%d".formatted(productId), cause);
    }
}

package com.example.order.exception;

public class InventoryUnavailableException extends RuntimeException {
    public InventoryUnavailableException(Long productId) {
        super("Stock insuficiente para el producto #%d".formatted(productId));
    }
}

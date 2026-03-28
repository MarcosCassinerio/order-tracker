package com.example.inventory.repository.inventory.exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long id) {
        super("Product #%d no encontrado".formatted(id));
    }
}

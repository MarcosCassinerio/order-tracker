package com.example.inventory.repository.inventory.dto;

import com.example.inventory.repository.inventory.model.Product;

import java.time.Instant;

public record ProductResponse(
        Long id,
        String name,
        Integer stock,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getStock(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}

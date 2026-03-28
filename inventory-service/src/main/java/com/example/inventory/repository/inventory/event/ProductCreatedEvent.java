package com.example.inventory.repository.inventory.event;

import com.example.inventory.repository.inventory.model.Product;

import java.time.Instant;

public record ProductCreatedEvent(
        Product product,
        Instant occurredAt
) {
    public ProductCreatedEvent(Product product) {
        this(product, Instant.now());
    }
}

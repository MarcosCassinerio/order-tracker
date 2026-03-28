package com.example.inventory.repository.inventory.dto;

import jakarta.validation.constraints.*;

public record CreateProductRequest(

        @NotNull(message = "name es requerido")
        String name,

        Integer stock
) {
    public CreateProductRequest {
        stock = stock == null ? 0 : stock;
    }
}

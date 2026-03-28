package com.example.inventory.repository.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record CreateProductRequest(

        @NotNull(message = "name es requerido")
        String name,

        BigDecimal stock
) {
    public CreateProductRequest {
        stock = stock == null ? BigDecimal.ZERO: stock;
    }
}

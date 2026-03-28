package com.example.inventory.repository.inventory.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateStockRequest(
        @NotNull(message = "El stock a reservar/liberar es requerido")
        Integer stock
) {}

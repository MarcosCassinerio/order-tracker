package com.example.inventory.repository.inventory.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateStockRequest(
        @NotNull(message = "El stock a reservar/liberar es requerido")
        BigDecimal stock
) {}

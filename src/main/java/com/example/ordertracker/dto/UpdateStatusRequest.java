package com.example.ordertracker.dto;

import com.example.ordertracker.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
        @NotNull(message = "El nuevo status es requerido")
        OrderStatus newStatus
) {}

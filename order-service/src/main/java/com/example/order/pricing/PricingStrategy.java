package com.example.order.pricing;

import java.math.BigDecimal;

/**
 * PATRÓN STRATEGY
 *
 * Por qué Strategy en vez de un if-else en el servicio:
 *   - Open/Closed: agregar un nuevo tipo de precio = nueva clase, sin tocar el servicio.
 *   - Testeable: cada estrategia se prueba en aislamiento.
 *   - Legible: el nombre de la clase documenta la intención.
 *
 * Por qué no un enum: un enum no puede tener comportamiento complejo
 * ni inyectar dependencias (ej: HolidayPricing podría consultar un HolidayCalendarService).
 */
public interface PricingStrategy {

    /**
     * Calcula el precio final dado un subtotal bruto.
     *
     * @param subtotal  Suma de todos los ítems sin descuento
     * @return          Precio final (con o sin descuento según la estrategia)
     */
    BigDecimal calculate(BigDecimal subtotal);

    /** Nombre de la estrategia — usado por el Factory para seleccionarla */
    String getName();
}

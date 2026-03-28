package com.example.ordertracker.pricing;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * PATRÓN FACTORY (combinado con Strategy)
 *
 * Spring inyecta automáticamente TODOS los beans que implementan PricingStrategy.
 * Los registramos en un Map<nombre → estrategia> para lookup O(1).
 *
 * Ventaja: agregar una nueva estrategia (ej: VipPricing) solo requiere:
 *   1. Crear la clase con @Component("VIP")
 *   2. No tocar el Factory ni el Service → Open/Closed en acción
 */
@Component
public class PricingStrategyFactory {

    private final Map<String, PricingStrategy> strategies;

    // Spring detecta todas las implementaciones de PricingStrategy en el classpath
    // y las inyecta aquí como List. El Factory las indexa por nombre.
    public PricingStrategyFactory(List<PricingStrategy> strategies) {
        this.strategies = strategies.stream()
                .collect(Collectors.toMap(
                        PricingStrategy::getName,
                        s -> s
                ));
    }

    /**
     * Retorna la estrategia correspondiente al tipo solicitado.
     * Si no existe, usa REGULAR como fallback (fail-safe).
     */
    public PricingStrategy get(String type) {
        return Optional.ofNullable(strategies.get(type))
                .orElseGet(() -> strategies.get("REGULAR"));
    }

    /** Para tests: verificar qué estrategias están registradas */
    public boolean supports(String type) {
        return strategies.containsKey(type);
    }
}

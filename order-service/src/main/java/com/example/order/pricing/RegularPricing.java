package com.example.order.pricing;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

// ─────────────────────────────────────────────────────────────────────────────
// ESTRATEGIA 1: Precio regular sin descuento
// ─────────────────────────────────────────────────────────────────────────────
@Component("REGULAR")
public class RegularPricing implements PricingStrategy {

    @Override
    public BigDecimal calculate(BigDecimal subtotal) {
        return subtotal.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getName() {
        return "REGULAR";
    }
}

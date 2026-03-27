package com.example.ordertracker.pricing;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

// ─────────────────────────────────────────────────────────────────────────────
// ESTRATEGIA 2: Descuento de feriados/promociones (20% off)
// ─────────────────────────────────────────────────────────────────────────────
@Component("HOLIDAY")
public class HolidayPricing implements PricingStrategy {

    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.80"); // 20% off

    @Override
    public BigDecimal calculate(BigDecimal subtotal) {
        return subtotal
                .multiply(DISCOUNT_RATE)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getName() {
        return "HOLIDAY";
    }
}

package com.example.order.service;

import com.example.order.pricing.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test de las estrategias de pricing.
 *
 * Estos tests son extremadamente rápidos (sin Spring, sin DB, sin mocks).
 * Testean la lógica matemática de cada estrategia en aislamiento total.
 *
 * Demostran dos cosas importantes para la entrevista:
 *   1. El patrón Strategy es fácilmente testeable en aislamiento.
 *   2. @ParameterizedTest elimina código duplicado (DRY en los tests).
 */
@DisplayName("Pricing Strategies — Unit Tests")
class PricingStrategyTest {

    // ── RegularPricing ────────────────────────────────────────────────────────

    @Test
    @DisplayName("RegularPricing devuelve el subtotal sin modificar")
    void regularPricing_returnsSubtotalUnchanged() {
        var strategy = new RegularPricing();
        var subtotal  = new BigDecimal("1050.00");

        var result = strategy.calculate(subtotal);

        assertThat(result).isEqualByComparingTo("1050.00");
    }

    @ParameterizedTest(name = "subtotal={0} → total={0} (sin descuento)")
    @CsvSource({
            "100.00, 100.00",
            "0.01,   0.01",
            "999.99, 999.99",
            "5000.00, 5000.00"
    })
    @DisplayName("RegularPricing — varios montos")
    void regularPricing_multipleAmounts(String input, String expected) {
        var strategy = new RegularPricing();
        assertThat(strategy.calculate(new BigDecimal(input)))
                .isEqualByComparingTo(expected);
    }

    // ── HolidayPricing ────────────────────────────────────────────────────────

    @Test
    @DisplayName("HolidayPricing aplica 20% de descuento")
    void holidayPricing_applies20PercentDiscount() {
        var strategy = new HolidayPricing();
        var subtotal  = new BigDecimal("1000.00");

        var result = strategy.calculate(subtotal);

        // 1000 × 0.80 = 800
        assertThat(result).isEqualByComparingTo("800.00");
    }

    @ParameterizedTest(name = "subtotal={0} → total={1} (20% off)")
    @CsvSource({
            "100.00,  80.00",
            "50.00,   40.00",
            "333.33, 266.66",   // verifica el redondeo HALF_UP
            "1.00,    0.80"
    })
    @DisplayName("HolidayPricing — varios montos con redondeo")
    void holidayPricing_multipleAmounts(String input, String expected) {
        var strategy = new HolidayPricing();
        assertThat(strategy.calculate(new BigDecimal(input)))
                .isEqualByComparingTo(expected);
    }

    // ── PricingStrategyFactory ────────────────────────────────────────────────

    @Test
    @DisplayName("Factory devuelve RegularPricing para tipo 'REGULAR'")
    void factory_returnsRegularForRegularType() {
        var factory = new PricingStrategyFactory(
                List.of(new RegularPricing(), new HolidayPricing())
        );

        assertThat(factory.get("REGULAR")).isInstanceOf(RegularPricing.class);
    }

    @Test
    @DisplayName("Factory devuelve HolidayPricing para tipo 'HOLIDAY'")
    void factory_returnsHolidayForHolidayType() {
        var factory = new PricingStrategyFactory(
                List.of(new RegularPricing(), new HolidayPricing())
        );

        assertThat(factory.get("HOLIDAY")).isInstanceOf(HolidayPricing.class);
    }

    @Test
    @DisplayName("Factory hace fallback a REGULAR si el tipo no existe")
    void factory_fallsBackToRegularForUnknownType() {
        var factory = new PricingStrategyFactory(
                List.of(new RegularPricing(), new HolidayPricing())
        );

        // Un tipo inventado → debe devolver REGULAR, no lanzar excepción
        assertThat(factory.get("TIPO_INEXISTENTE"))
                .isInstanceOf(RegularPricing.class);
    }
}

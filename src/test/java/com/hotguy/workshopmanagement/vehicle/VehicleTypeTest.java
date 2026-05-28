package com.hotguy.workshopmanagement.vehicle;

import com.hotguy.workshopmanagement.vehicle.model.VehicleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios del enum {@link VehicleType} y su lógica de facturación.
 *
 * <p>
 * Usa {@code @ParameterizedTest} con {@code @CsvSource} para ejecutar el mismo
 * test con diferentes combinaciones de datos, evitando repetición de código.
 */
@DisplayName("VehicleType - Tests de facturación")
class VehicleTypeTest {

    @Test
    @DisplayName("Motocicleta: 20€/hora, 0€ fijo")
    void motorcycleRates() {
        assertThat(VehicleType.MOTORCYCLE.getHourlyRate()).isEqualTo(20f);
        assertThat(VehicleType.MOTORCYCLE.getFixedFee()).isZero();
    }

    @Test
    @DisplayName("Coche: 25€/hora, 0€ fijo")
    void carRates() {
        assertThat(VehicleType.CAR.getHourlyRate()).isEqualTo(25f);
        assertThat(VehicleType.CAR.getFixedFee()).isZero();
    }

    @Test
    @DisplayName("Furgoneta: 30€/hora, 30€ fijo")
    void vanRates() {
        assertThat(VehicleType.VAN.getHourlyRate()).isEqualTo(30f);
        assertThat(VehicleType.VAN.getFixedFee()).isEqualTo(30f);
    }

    @Test
    @DisplayName("Camión: 40€/hora, 50€ fijo")
    void truckRates() {
        assertThat(VehicleType.TRUCK.getHourlyRate()).isEqualTo(40f);
        assertThat(VehicleType.TRUCK.getFixedFee()).isEqualTo(50f);
    }

    /**
     * Test parametrizado que verifica el cálculo de precio para múltiples
     * combinaciones.
     * Cada fila de @CsvSource es: tipo, horas, precio_esperado.
     */
    @ParameterizedTest(name = "{0}: {1}h → {2}€")
    @CsvSource({
            "MOTORCYCLE, 5,  100.0", // 5 × 20 + 0
            "CAR,        4,  100.0", // 4 × 25 + 0
            "VAN,        2,   90.0", // 2 × 30 + 30
            "VAN,        0,   30.0", // 0 × 30 + 30 (solo tarifa fija)
            "TRUCK,      2,  130.0", // 2 × 40 + 50
            "TRUCK,      0,   50.0", // 0 × 40 + 50 (solo tarifa fija)
            "MOTORCYCLE, 0,    0.0", // 0 × 20 + 0
    })
    @DisplayName("Cálculo de precio parametrizado")
    void priceCalculation(String typeStr, float hours, float expectedPrice) {
        VehicleType type = VehicleType.valueOf(typeStr);
        assertThat(type.calculatePrice(hours)).isEqualTo(expectedPrice);
    }

    @Test
    @DisplayName("Horas negativas deben lanzar IllegalArgumentException")
    void negativeHoursShouldThrow() {
        assertThatThrownBy(() -> VehicleType.CAR.calculatePrice(-1f))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

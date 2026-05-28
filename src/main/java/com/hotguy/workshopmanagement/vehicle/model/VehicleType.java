package com.hotguy.workshopmanagement.vehicle.model;

/**
 * Tipos de vehículo soportados por el sistema, con sus tarifas de facturación.
 *
 * <p>
 * Cada tipo define una tarifa horaria ({@code hourlyRate}) y una tarifa fija
 * por dificultad ({@code fixedFee}) que se aplica independientemente de las
 * horas.
 * La fórmula de facturación es: {@code (horas × hourlyRate) + fixedFee}.
 *
 * <p>
 * Al almacenar el enum en la BD se guarda el {@code name()} (el texto del enum,
 * ej. "CAR"), no el ordinal numérico. Esto hace el esquema legible y robusto
 * frente a reordenaciones del enum.
 */
public enum VehicleType {

    /**
     * Motocicleta. Tarifa: 20€/hora, sin cargo fijo.
     */
    MOTORCYCLE(20f, 0f, "Motocicleta"),

    /**
     * Turismo / coche particular. Tarifa: 25€/hora, sin cargo fijo.
     */
    CAR(25f, 0f, "Coche"),

    /**
     * Furgoneta. Tarifa: 30€/hora + 30€ fijo por dificultad.
     */
    VAN(30f, 30f, "Furgoneta"),

    /**
     * Camión. Tarifa: 40€/hora + 50€ fijo por dificultad.
     */
    TRUCK(40f, 50f, "Camión");

    /**
     * Precio por hora de mano de obra en euros.
     */
    private final float hourlyRate;

    /**
     * Cargo fijo adicional en euros (por complejidad del vehículo).
     */
    private final float fixedFee;

    /**
     * Nombre legible para mostrar en la interfaz.
     */
    private final String displayName;

    VehicleType(float hourlyRate, float fixedFee, String displayName) {
        this.hourlyRate = hourlyRate;
        this.fixedFee = fixedFee;
        this.displayName = displayName;
    }

    public float getHourlyRate() {
        return hourlyRate;
    }

    public float getFixedFee() {
        return fixedFee;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Calcula el precio total para las horas indicadas.
     *
     * @param hours horas trabajadas (no negativas)
     * @return precio total en euros
     * @throws IllegalArgumentException si las horas son negativas
     */
    public float calculatePrice(float hours) {
        if (hours < 0) {
            throw new IllegalArgumentException("Las horas no pueden ser negativas");
        }
        return (hours * hourlyRate) + fixedFee;
    }
}

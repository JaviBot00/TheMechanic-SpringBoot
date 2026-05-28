package com.workshopmanagement.vehicle.dto;

import com.workshopmanagement.vehicle.model.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de petición para crear o actualizar un vehículo.
 *
 * @param registrationCode matrícula del vehículo
 * @param model            marca y modelo
 * @param type             tipo de vehículo (CAR, MOTORCYCLE, VAN, TRUCK)
 * @param clientId         ID del propietario
 */
public record VehicleRequest(
        @NotBlank(message = "La matrícula es obligatoria")
        @Size(max = 20, message = "La matrícula no puede superar 20 caracteres")
        String registrationCode,

        @NotBlank(message = "El modelo es obligatorio")
        @Size(max = 150)
        String model,

        @NotNull(message = "El tipo de vehículo es obligatorio")
        VehicleType type,

        @NotNull(message = "El ID del propietario es obligatorio")
        Long clientId
) {}

package com.hotguy.workshopmanagement.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO de petición para crear una nueva orden de trabajo.
 *
 * @param vehicleId    ID del vehículo a reparar
 * @param mechanicId   ID del mecánico asignado
 * @param diagnostic   descripción del problema
 * @param previewHours horas estimadas de trabajo
 * @param initDate     fecha de inicio
 * @param notes        notas adicionales (opcional)
 */
public record WorkshopTaskRequest(
//    @NotNull(message = "El ID del vehículo es obligatorio")
    Long vehicleId,

//    @NotNull(message = "El ID del mecánico es obligatorio")
    Long mechanicId,

    @NotBlank(message = "El diagnóstico es obligatorio") @Size(max = 500) String diagnostic,

//    @NotNull(message = "Las horas estimadas son obligatorias")
    @Positive(message = "Las horas estimadas deben ser positivas") Float previewHours,

//    @NotNull(message = "La fecha de inicio es obligatoria")
    LocalDate initDate,

    @Size(max = 500) String notes) {
}

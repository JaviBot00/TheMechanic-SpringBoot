package com.workshopmanagement.task.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de petición para añadir horas de trabajo a una tarea.
 *
 * @param hours horas a añadir (deben ser positivas)
 */
public record AddHoursRequest(
        @NotNull(message = "Las horas son obligatorias")
        @Positive(message = "Las horas deben ser un valor positivo")
        Float hours
) {}

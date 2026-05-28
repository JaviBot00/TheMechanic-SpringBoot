package com.hotguy.workshopmanagement.mechanic.dto;

import java.time.Instant;
import java.time.LocalDate;

/**
 * DTO de respuesta para operaciones sobre mecánicos.
 *
 * @param id               identificador técnico
 * @param name             nombre de pila
 * @param surname1         primer apellido
 * @param surname2         segundo apellido
 * @param nif              NIF
 * @param email            email
 * @param telephone        teléfono
 * @param registrationDate fecha de incorporación
 * @param specialty        especialidad
 * @param taskCount        número de tareas asignadas actualmente
 * @param createdAt        fecha de alta en el sistema
 * @param updatedAt        fecha de última modificación
 */
public record MechanicResponse(
    Long id,
    String name,
    String surname1,
    String surname2,
    String nif,
    String email,
    String telephone,
    LocalDate registrationDate,
    String specialty,
    int taskCount,
    Instant createdAt,
    Instant updatedAt) {
}

package com.hotguy.workshopmanagement.task.dto;

import java.time.Instant;
import java.time.LocalDate;

/**
 * DTO de respuesta para operaciones sobre tareas de taller.
 *
 * @param id            identificador técnico
 * @param diagnostic    descripción del problema
 * @param solution      solución aplicada
 * @param previewHours  horas estimadas
 * @param realHours     horas reales trabajadas
 * @param progress      porcentaje de progreso (0-100)
 * @param status        estado legible: Pendiente, En progreso, Finalizada,
 *                      Pagada
 * @param estimatedCost coste estimado en euros
 * @param totalCost     coste real en euros (0 si no finalizada)
 * @param finished      si la tarea está finalizada
 * @param paid          si la tarea está pagada
 * @param initDate      fecha de inicio
 * @param notes         notas del mecánico
 * @param clientId      ID del cliente
 * @param clientName    nombre del cliente
 * @param vehicleId     ID del vehículo
 * @param vehicleReg    matrícula del vehículo
 * @param mechanicId    ID del mecánico
 * @param mechanicName  nombre del mecánico
 * @param createdAt     fecha de creación
 * @param updatedAt     fecha de última modificación
 */
public record WorkshopTaskResponse(
    Long id,
    String diagnostic,
    String solution,
    float previewHours,
    float realHours,
    float progress,
    String status,
    float estimatedCost,
    float totalCost,
    boolean finished,
    boolean paid,
    LocalDate initDate,
    String notes,
    Long clientId,
    String clientName,
    Long vehicleId,
    String vehicleReg,
    Long mechanicId,
    String mechanicName,
    Instant createdAt,
    Instant updatedAt) {
}

package com.hotguy.workshopmanagement.vehicle.dto;

import com.hotguy.workshopmanagement.vehicle.model.VehicleType;
import java.time.Instant;

/**
 * DTO de respuesta para operaciones sobre vehículos.
 *
 * @param id               identificador técnico
 * @param registrationCode matrícula
 * @param model            marca y modelo
 * @param type             tipo de vehículo
 * @param hourlyRate       tarifa por hora del tipo de vehículo
 * @param fixedFee         cargo fijo del tipo de vehículo
 * @param clientId         ID del propietario
 * @param clientName       nombre del propietario (para evitar un join extra en
 *                         el frontend)
 * @param taskCount        número de tareas asociadas
 * @param completionPct    porcentaje de tareas completadas
 * @param totalRevenue     facturación total generada
 * @param createdAt        fecha de alta
 * @param updatedAt        fecha de última modificación
 */
public record VehicleResponse(
        Long id,
        String registrationCode,
        String model,
        VehicleType type,
        float hourlyRate,
        float fixedFee,
        Long clientId,
        String clientName,
        int taskCount,
        float completionPct,
        float totalRevenue,
        Instant createdAt,
        Instant updatedAt) {
}

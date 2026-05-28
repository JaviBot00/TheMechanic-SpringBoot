package com.hotguy.workshopmanagement.task.mapper;

import com.hotguy.workshopmanagement.task.dto.WorkshopTaskRequest;
import com.hotguy.workshopmanagement.task.dto.WorkshopTaskResponse;
import com.hotguy.workshopmanagement.task.model.WorkshopTask;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper MapStruct para conversión entre {@link WorkshopTask} y sus DTOs.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface WorkshopTaskMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "client", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "mechanic", ignore = true)
    @Mapping(target = "realHours", ignore = true)
    @Mapping(target = "finished", ignore = true)
    @Mapping(target = "paid", ignore = true)
    @Mapping(target = "solution", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    WorkshopTask toEntity(WorkshopTaskRequest request);

    @Mapping(target = "progress", expression = "java(task.getProgress())")
    @Mapping(target = "status", expression = "java(task.getStatus())")
    @Mapping(target = "estimatedCost", expression = "java(task.getEstimatedCost())")
    @Mapping(target = "totalCost", expression = "java(task.getTotalCost())")
    @Mapping(target = "clientId", expression = "java(task.getClient().getId())")
    @Mapping(target = "clientName", expression = "java(task.getClient().getName() + ' ' + task.getClient().getSurname1())")
    @Mapping(target = "vehicleId", expression = "java(task.getVehicle().getId())")
    @Mapping(target = "vehicleReg", expression = "java(task.getVehicle().getRegistrationCode())")
    @Mapping(target = "mechanicId", expression = "java(task.getMechanic().getId())")
    @Mapping(target = "mechanicName", expression = "java(task.getMechanic().getName() + ' ' + task.getMechanic().getSurname1())")
    WorkshopTaskResponse toResponse(WorkshopTask task);
}

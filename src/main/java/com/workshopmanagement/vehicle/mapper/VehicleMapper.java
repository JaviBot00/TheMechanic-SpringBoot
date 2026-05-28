package com.workshopmanagement.vehicle.mapper;

import com.workshopmanagement.vehicle.dto.VehicleRequest;
import com.workshopmanagement.vehicle.dto.VehicleResponse;
import com.workshopmanagement.vehicle.model.Vehicle;
import org.mapstruct.*;

/**
 * Mapper MapStruct para conversión entre {@link Vehicle} y sus DTOs.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface VehicleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "proprietary", ignore = true)
    @Mapping(target = "workshopTasks", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Vehicle toEntity(VehicleRequest request);

    @Mapping(target = "hourlyRate", expression = "java(vehicle.getType().getHourlyRate())")
    @Mapping(target = "fixedFee", expression = "java(vehicle.getType().getFixedFee())")
    @Mapping(target = "clientId", expression = "java(vehicle.getProprietary().getId())")
    @Mapping(target = "clientName", expression = "java(vehicle.getProprietary().getName() + ' ' + vehicle.getProprietary().getSurname1())")
    @Mapping(target = "taskCount", expression = "java(vehicle.getWorkshopTasks().size())")
    @Mapping(target = "completionPct", expression = "java(vehicle.getCompletionPercentage())")
    @Mapping(target = "totalRevenue", expression = "java(vehicle.getTotalRevenue())")
    VehicleResponse toResponse(Vehicle vehicle);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "proprietary", ignore = true)
    @Mapping(target = "workshopTasks", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntityFromRequest(VehicleRequest request, @MappingTarget Vehicle vehicle);
}

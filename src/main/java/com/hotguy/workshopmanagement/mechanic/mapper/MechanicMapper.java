package com.hotguy.workshopmanagement.mechanic.mapper;

import com.hotguy.workshopmanagement.mechanic.dto.MechanicRequest;
import com.hotguy.workshopmanagement.mechanic.dto.MechanicResponse;
import com.hotguy.workshopmanagement.mechanic.model.Mechanic;
import org.mapstruct.*;

/**
 * Mapper MapStruct para conversión entre {@link Mechanic} y sus DTOs.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MechanicMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "workshopTasks", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Mechanic toEntity(MechanicRequest request);

    @Mapping(target = "taskCount", expression = "java(mechanic.getWorkshopTasks().size())")
    MechanicResponse toResponse(Mechanic mechanic);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "workshopTasks", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntityFromRequest(MechanicRequest request, @MappingTarget Mechanic mechanic);
}

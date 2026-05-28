package com.hotguy.workshopmanagement.client.mapper;

import com.hotguy.workshopmanagement.client.dto.ClientRequest;
import com.hotguy.workshopmanagement.client.dto.ClientResponse;
import com.hotguy.workshopmanagement.client.model.Client;
import org.mapstruct.*;

/**
 * Mapper MapStruct para conversión entre la entidad {@link Client} y sus DTOs.
 *
 * <p>
 * MapStruct genera en tiempo de compilación el código de conversión entre
 * objetos. Es más eficiente que frameworks de reflection como ModelMapper
 * porque
 * el código generado es Java plano, sin reflection en runtime.
 *
 * <p>
 * {@code componentModel = "spring"}: el mapper generado es un bean de Spring
 * ({@code @Component}), inyectable con {@code @Autowired} o
 * {@code @RequiredArgsConstructor}.
 * Este comportamiento se configura globalmente en el {@code pom.xml} con
 * {@code -Amapstruct.defaultComponentModel=spring}, por lo que podría omitirse
 * aquí,
 * pero lo dejamos explícito por claridad.
 *
 * <p>
 * {@code unmappedTargetPolicy = ERROR}: si hay campos del destino que no se
 * mapean,
 * la compilación falla. Esto previene que olvidemos mapear campos nuevos.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ClientMapper {

    /**
     * Convierte un DTO de petición a entidad JPA.
     *
     * <p>
     * {@code @Mapping(target = "id", ignore = true)}: el ID lo genera la BD,
     * no viene del cliente. Ignoramos estos campos del destino.
     *
     * @param request el DTO con los datos del cliente
     * @return la entidad JPA lista para persistir
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vehicles", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Client toEntity(ClientRequest request);

    /**
     * Convierte una entidad JPA a DTO de respuesta.
     *
     * <p>
     * {@code expression}: cuando el campo del destino no existe directamente
     * en la entidad, usamos una expresión Java para calcularlo.
     * Aquí {@code vehicleCount} se calcula como el tamaño de la lista de vehículos.
     *
     * @param client la entidad JPA
     * @return el DTO de respuesta listo para serializar a JSON
     */
    @Mapping(target = "vehicleCount", expression = "java(client.getVehicles().size())")
    ClientResponse toResponse(Client client);

    /**
     * Actualiza una entidad existente con los datos del DTO de petición.
     *
     * <p>
     * {@code @MappingTarget}: indica que este parámetro es el objeto destino
     * que se va a modificar en lugar de crear uno nuevo. MapStruct solo actualiza
     * los campos mapeados, dejando los demás intactos (ej. {@code id},
     * {@code createdAt}).
     *
     * <p>
     * {@code NullValuePropertyMappingStrategy.IGNORE}: si un campo del DTO es null,
     * no sobreescribe el valor actual de la entidad. Útil para actualizaciones
     * parciales.
     *
     * @param request los nuevos datos
     * @param client  la entidad a actualizar
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vehicles", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntityFromRequest(ClientRequest request, @MappingTarget Client client);
}

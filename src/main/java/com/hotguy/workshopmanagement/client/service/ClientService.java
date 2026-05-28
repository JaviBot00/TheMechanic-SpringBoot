package com.hotguy.workshopmanagement.client.service;

import com.hotguy.workshopmanagement.client.dto.ClientRequest;
import com.hotguy.workshopmanagement.client.dto.ClientResponse;
import com.hotguy.workshopmanagement.client.mapper.ClientMapper;
import com.hotguy.workshopmanagement.client.model.Client;
import com.hotguy.workshopmanagement.client.repository.ClientRepository;
import com.hotguy.workshopmanagement.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio que contiene la lógica de negocio para la gestión de clientes.
 *
 * <p>
 * Esta capa actúa como intermediaria entre el Controller (que maneja HTTP)
 * y el Repository (que accede a la BD). La lógica de negocio nunca va en el
 * Controller ni en el Repository.
 *
 * <p>
 * {@code @Transactional}: todos los métodos públicos se ejecutan dentro de
 * una transacción. Si algo falla, los cambios se revierten automáticamente.
 * Los métodos de solo lectura usan {@code readOnly = true} para optimizar.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    /**
     * Crea un nuevo cliente en el sistema.
     *
     * <p>
     * {@code @PreAuthorize("hasAnyRole('ADMIN','MECHANIC')")}:
     * Spring Security evalúa esta expresión antes de ejecutar el método.
     * Si el usuario no tiene el rol requerido, lanza {@code AccessDeniedException}
     * que el {@code GlobalExceptionHandler} convierte en 403 Forbidden.
     *
     * @param request datos del nuevo cliente
     * @return el cliente creado como DTO de respuesta
     * @throws IllegalArgumentException si el NIF o código ya existe
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'MECHANIC')")
    public ClientResponse createClient(ClientRequest request) {
        if (clientRepository.existsByNif(request.nif())) {
            throw new IllegalArgumentException("Ya existe un cliente con el NIF: " + request.nif());
        }
        if (clientRepository.existsByClientCode(request.clientCode())) {
            throw new IllegalArgumentException("Ya existe un cliente con el código: " + request.clientCode());
        }
        Client client = clientMapper.toEntity(request);
        return clientMapper.toResponse(clientRepository.save(client));
    }

    /**
     * Obtiene un cliente por su ID.
     *
     * <p>
     * Un CLIENT solo puede ver sus propios datos. La expresión SpEL
     * {@code @preAuthorize} accede al SecurityContext para obtener el usuario
     * actual
     * y verifica si su ID vinculado coincide.
     *
     * @param id identificador del cliente
     * @return el cliente como DTO de respuesta
     * @throws ResourceNotFoundException si no existe
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','MECHANIC') or " +
        "(hasRole('CLIENT') and @clientSecurityService.isOwner(authentication, #id))")
    public ClientResponse getClientById(Long id) {
        return clientMapper.toResponse(findClientOrThrow(id));
    }

    /**
     * Lista todos los clientes activos con paginación.
     *
     * @param pageable configuración de página, tamaño y ordenación
     * @return página de clientes
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'MECHANIC')")
    public Page<ClientResponse> listClients(Pageable pageable) {
        return clientRepository.findAll(pageable).map(clientMapper::toResponse);
    }

    /**
     * Busca clientes por primer apellido con paginación.
     *
     * @param surname1 texto a buscar
     * @param pageable paginación
     * @return página de clientes que coinciden
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'MECHANIC')")
    public Page<ClientResponse> findClientsBySurname(String surname1, Pageable pageable) {
        return clientRepository.findBySurname1ContainingIgnoreCase(surname1, pageable)
            .map(clientMapper::toResponse);
    }

    /**
     * Busca un cliente por su NIF.
     *
     * @param nif el NIF del cliente
     * @return el cliente como DTO
     * @throws ResourceNotFoundException si no existe
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'MECHANIC')")
    public ClientResponse findClientByNif(String nif) {
        return clientRepository.findByNif(nif)
            .map(clientMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con NIF: " + nif));
    }

    /**
     * Actualiza los datos de un cliente existente.
     *
     * @param id      identificador del cliente a actualizar
     * @param request nuevos datos
     * @return el cliente actualizado
     * @throws ResourceNotFoundException si no existe
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'MECHANIC')")
    public ClientResponse updateClient(Long id, ClientRequest request) {
        Client client = findClientOrThrow(id);
        // Comprobar NIF duplicado solo si está cambiando
        if (!client.getNif().equals(request.nif()) && clientRepository.existsByNif(request.nif())) {
            throw new IllegalArgumentException("El NIF ya está en uso: " + request.nif());
        }
        clientMapper.updateEntityFromRequest(request, client);
        return clientMapper.toResponse(clientRepository.save(client));
    }

    /**
     * Elimina lógicamente un cliente (soft delete).
     * El registro permanece en BD con {@code deletedAt} relleno.
     *
     * @param id identificador del cliente
     * @throws ResourceNotFoundException si no existe
     */
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteClient(Long id) {
        Client client = findClientOrThrow(id);
        client.softDelete();
        clientRepository.save(client);
    }

    /**
     * Devuelve el número total de clientes activos.
     * Usado en reportes y estadísticas.
     *
     * @return número de clientes
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'MECHANIC')")
    public long countClients() {
        return clientRepository.countActiveClients();
    }

    // =========================================================================
    // MÉTODOS PRIVADOS
    // =========================================================================

    /**
     * Busca un cliente por ID o lanza {@link ResourceNotFoundException}.
     * Patrón de uso frecuente en servicios para evitar código repetido.
     */
    private Client findClientOrThrow(Long id) {
        return clientRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + id));
    }
}
